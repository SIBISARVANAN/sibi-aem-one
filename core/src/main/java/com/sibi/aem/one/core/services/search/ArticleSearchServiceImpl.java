package com.sibi.aem.one.core.services.search;

import com.day.cq.search.Predicate;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.eval.JcrPropertyPredicateEvaluator;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.*;

/**
 * ================================================================
 * ArticleSearchServiceImpl
 * ================================================================
 *
 * This service uses TWO complementary approaches to build queries,
 * both of which appear in real codebases and interviews:
 *
 *  APPROACH A — Map<String,String> + PredicateGroup.create(map)
 *    Used for simple, flat predicates and numbered group syntax.
 *    Easy to read, easy to debug via /bin/querybuilder.json.
 *
 *  APPROACH B — PredicateGroup / Predicate Java API
 *    Used for complex AND/OR trees that are hard to express
 *    with numbered string keys. More type-safe and composable.
 *
 * Predicates covered (matches the interview guide exactly):
 *
 *   [A]  type                   — cq:Page, always first
 *   [A]  path                   — root scope
 *   [A]  path.exclude           — exclude /drafts sub-tree
 *   [A]  nodename               — glob filter on node name
 *   [A]  1_property             — resource type pin
 *   [A]  daterange              — publishDate lower + upper bound
 *   [A]  fulltext + relPath     — full-text scoped to jcr:content
 *   [A]  tagid + tagid.property — tag matching (multiple = OR)
 *   [A]  orderby + orderby.sort — sorting
 *   [A]  p.limit / p.offset     — pagination
 *   [A]  p.guessTotal           — performance-safe result count
 *
 *   [B]  property (multi-value, OR) — categories
 *   [B]  property (multi-value, OR) — authors
 *   [B]  property (multi-value, OR) — templates
 *   [B]  property (single value)    — featured=true
 *   [B]  NOT group                  — exclude articleStatus=archived
 *
 * Why split them this way?
 *   Flat predicates (date, fulltext, path, nodename, tagid) are
 *   simple enough to express as string keys in a Map.
 *   Multi-value OR groups and nested NOT groups are much cleaner
 *   with the PredicateGroup API — no manual group numbering,
 *   no string concatenation, no off-by-one bugs.
 *
 * ================================================================
 */
@Component(service = ArticleSearchService.class)
public class ArticleSearchServiceImpl implements ArticleSearchService {

    private static final Logger log =
            LoggerFactory.getLogger(ArticleSearchServiceImpl.class);

    @Reference
    private QueryBuilder queryBuilder;

    // -------------------------------------------------------
    // JCR property paths
    // -------------------------------------------------------
    private static final String PROP_RESOURCE_TYPE = "jcr:content/sling:resourceType";
    private static final String PROP_PUBLISH_DATE  = "jcr:content/publishDate";
    private static final String PROP_AUTHOR        = "jcr:content/authorId";
    private static final String PROP_CATEGORY      = "jcr:content/category";
    private static final String PROP_TAGS          = "jcr:content/cq:tags";
    private static final String PROP_STATUS        = "jcr:content/articleStatus";
    private static final String PROP_FEATURED      = "jcr:content/featured";
    private static final String PROP_TEMPLATE      = "jcr:content/cq:template";

    private static final String ARTICLE_RESOURCE_TYPE =
            "sibi-aem-one/components/structure/article-page";
    private static final String STATUS_ARCHIVED = "archived";

    // ================================================================
    // Entry point
    // ================================================================
    @Override
    public ArticleSearchResult search(ArticleSearchRequest request,
                                      ResourceResolver resolver) {
        long start = System.currentTimeMillis();

        Session session = resolver.adaptTo(Session.class);
        if (session == null) {
            log.error("ArticleSearchService: could not adapt ResourceResolver to Session");
            return emptyResult(request);
        }

        try {
            // Build the root PredicateGroup that combines both approaches
            PredicateGroup root = buildQuery(request);

            logPredicates(root);

            Query query             = queryBuilder.createQuery(root, session);
            SearchResult result     = query.getResult();
            long queryTimeMs        = System.currentTimeMillis() - start;

            List<ArticleSearchResult.ArticleHit> hits   = mapHits(result);
            long total              = result.getTotalMatches();
            boolean hasMore         = (request.getPageNumber() * request.getPageSize()
                    + hits.size()) < total;

            log.debug("ArticleSearchService: {} hits (total≈{}) in {}ms",
                    hits.size(), total, queryTimeMs);

            return new ArticleSearchResult(hits, total, hasMore,
                    request.getPageNumber(), request.getPageSize(), queryTimeMs);

        } catch (Exception e) {
            log.error("ArticleSearchService: query failed", e);
            return emptyResult(request);
        }
        // NOTE: do NOT close session — owned by ResourceResolver
    }

    // ================================================================
    // STEP 1 — Build the complete PredicateGroup
    //
    // The root group is AND (allRequired = true by default).
    // We add flat predicates directly, and sub-groups for OR/NOT logic.
    // ================================================================
    private PredicateGroup buildQuery(ArticleSearchRequest req) {

        PredicateGroup root = new PredicateGroup();
        root.setAllRequired(true); // AND — every predicate must match

        // ============================================================
        // APPROACH A — flat predicates added as Map then merged,
        //              or added one by one as Predicate objects.
        //              Both styles are shown; pick one style per project.
        // ============================================================

        // [A-1] Node type — always declare first so Oak picks the right index
        root.add(simplePredicate("type", "cq:Page"));

        // [A-2] Path scope
        Predicate pathPred = new Predicate("path");
        pathPred.set("path", req.getRootPath());
        // path.self=true means the root path itself is also included in results
        pathPred.set("path.self", "false");
        root.add(pathPred);

        // [A-3] path.exclude — exclude a sub-tree (e.g. /drafts, /archived-archive)
        //       Note: path.exclude is a parameter on the path predicate,
        //       NOT a separate predicate — it must be set on the same Predicate object.
        if (StringUtils.isNotBlank(req.getExcludePath())) {
            pathPred.set("path.exclude", req.getExcludePath());
        }

        // [A-4] nodename — glob pattern matched against the JCR node name only
        //       Useful for URL-slug patterns, e.g. all articles whose node name
        //       starts with "2024-" → nodename = "2024-*"
        //       Supports * (any chars) and ? (single char) wildcards.
        if (StringUtils.isNotBlank(req.getNodeNamePattern())) {
            root.add(simplePredicate("nodename", req.getNodeNamePattern()));
        }

        // [A-5] Resource type — pins query to our article component
        //       Using 1_property notation via Predicate API equivalent
        Predicate resourceTypePred = new Predicate(JcrPropertyPredicateEvaluator.PROPERTY);
        resourceTypePred.set(JcrPropertyPredicateEvaluator.PROPERTY, PROP_RESOURCE_TYPE);
        resourceTypePred.set(JcrPropertyPredicateEvaluator.VALUE,    ARTICLE_RESOURCE_TYPE);
        root.add(resourceTypePred);

        // [A-6] Date range — publishDate between lowerBound and upperBound
        //       Both bounds are optional; only set what's provided.
        //       Note: always set lowerOperation/upperOperation explicitly —
        //       the default is ">" (strict), not ">=" (inclusive).
        if (StringUtils.isNotBlank(req.getPublishedAfter())
                || StringUtils.isNotBlank(req.getPublishedBefore())) {

            Predicate dateRange = new Predicate("daterange");
            dateRange.set("daterange.property", PROP_PUBLISH_DATE);

            if (StringUtils.isNotBlank(req.getPublishedAfter())) {
                dateRange.set("daterange.lowerBound",     req.getPublishedAfter());
                dateRange.set("daterange.lowerOperation", ">=");
            }
            if (StringUtils.isNotBlank(req.getPublishedBefore())) {
                dateRange.set("daterange.upperBound",     req.getPublishedBefore());
                dateRange.set("daterange.upperOperation", "<=");
            }
            root.add(dateRange);
        }

        // [A-7] Full-text search — scoped to jcr:content only
        //       WITHOUT relPath, QueryBuilder scans ALL child nodes including
        //       renditions, metadata nodes, etc. — very expensive on large repos.
        //       Always scope fulltext to the narrowest relevant sub-node.
        if (StringUtils.isNotBlank(req.getFullTextQuery())) {
            Predicate fullText = new Predicate("fulltext");
            fullText.set("fulltext",         req.getFullTextQuery());
            fullText.set("fulltext.relPath", "jcr:content");
            root.add(fullText);
        }

        // [A-8] Tag predicate — each tagid predicate matches one tag.
        //       Multiple tagid predicates in the same group are OR'd by QueryBuilder.
        //       Interview note: always set tagid.property — without it QueryBuilder
        //       defaults to cq:tags on the node itself (not jcr:content/cq:tags).
        if (req.getTags() != null && !req.getTags().isEmpty()) {
            for (String tag : req.getTags()) {
                Predicate tagPred = new Predicate("tagid");
                tagPred.set("tagid",          tag);
                tagPred.set("tagid.property", PROP_TAGS);
                root.add(tagPred);
            }
        }

        // [A-9] Ordering — sort by publishDate descending by default
        Predicate orderBy = new Predicate("orderby");
        orderBy.set("orderby",      req.getSortProperty());
        orderBy.set("orderby.sort", req.getSortOrder());
        root.add(orderBy);

        // [A-10] p.guessTotal — CRITICAL for performance
        //        Without this, QueryBuilder traverses every matching node to
        //        compute an exact count — catastrophic on large repos.
        //        With guessTotal=N, it stops counting at N and returns an estimate.
        //        Set it to pageSize*10 as a reasonable upper estimate.
        Predicate guessTotal = new Predicate("p.guessTotal");
        guessTotal.set("p.guessTotal", String.valueOf(req.getPageSize() * 10));
        root.add(guessTotal);

        // [A-11] Pagination — offset = pageNumber * pageSize
        Predicate pagination = new Predicate("p.limit");
        pagination.set("p.limit",  String.valueOf(req.getPageSize()));
        pagination.set("p.offset", String.valueOf(req.getPageNumber() * req.getPageSize()));
        root.add(pagination);

        // ============================================================
        // APPROACH B — PredicateGroup Java API for AND/OR/NOT trees
        //
        // The numbered string key approach (2_group.p.or=true etc.) works
        // but gets messy with 3+ nested groups. The PredicateGroup API
        // is composable, readable, and doesn't require manual numbering.
        // ============================================================

        // [B-1] Categories — OR across multiple values on the same property
        //       e.g. category = "technology" OR category = "sports"
        if (req.getCategories() != null && !req.getCategories().isEmpty()) {
            root.add(multiValueOrGroup(PROP_CATEGORY, req.getCategories()));
        }

        // [B-2] Authors — OR across multiple authors
        //       e.g. authorId = "alice" OR authorId = "bob"
        if (req.getAuthors() != null && !req.getAuthors().isEmpty()) {
            root.add(multiValueOrGroup(PROP_AUTHOR, req.getAuthors()));
        }

        // [B-3] Templates — OR across multiple page templates
        //       e.g. cq:template = "/conf/.../article" OR "/conf/.../blog"
        //       Interview note: multi-value property with operation=equals
        //       is different from the default "contains" — always be explicit.
        if (req.getTemplates() != null && !req.getTemplates().isEmpty()) {
            root.add(multiValueOrGroup(PROP_TEMPLATE, req.getTemplates()));
        }

        // [B-4] Featured flag — simple single-value property predicate
        //       This is the CORRECT way to filter on a boolean property.
        //       A custom PredicateEvaluator would be wrong here because
        //       the built-in property predicate handles this perfectly,
        //       and it can be pushed down to the Oak/XPath layer (faster).
        if (req.isFeaturedOnly()) {
            Predicate featuredPred = new Predicate(JcrPropertyPredicateEvaluator.PROPERTY);
            featuredPred.set(JcrPropertyPredicateEvaluator.PROPERTY, PROP_FEATURED);
            featuredPred.set(JcrPropertyPredicateEvaluator.VALUE,    "true");
            root.add(featuredPred);
        }

        // [B-5] Exclude archived — NOT group
        //       A NOT group wraps any sub-predicate and inverts it.
        //       allRequired=true + p.not=true means: exclude nodes where
        //       ALL conditions in this group match (i.e. exclude archived).
        if (req.isExcludeArchived()) {
            PredicateGroup notArchived = new PredicateGroup();
            notArchived.setAllRequired(true);
            notArchived.setNegated(true);   // ← this is the NOT operator

            Predicate archivedPred = new Predicate(JcrPropertyPredicateEvaluator.PROPERTY);
            archivedPred.set(JcrPropertyPredicateEvaluator.PROPERTY, PROP_STATUS);
            archivedPred.set(JcrPropertyPredicateEvaluator.VALUE,    STATUS_ARCHIVED);

            notArchived.add(archivedPred);
            root.add(notArchived);
        }

        return root;
    }

    // ================================================================
    // Helper — builds an OR PredicateGroup for multiple values
    //          on the same JCR property.
    //
    //  e.g. multiValueOrGroup("jcr:content/category", ["tech","sports"])
    //  produces:
    //    group (allRequired=false = OR)
    //      property = jcr:content/category, value = tech
    //      property = jcr:content/category, value = sports
    //
    // This is the reusable pattern from the "Boolean logic" section of
    // the interview guide. Extract it as a method so categories, authors,
    // and templates all share the same logic without copy-paste.
    // ================================================================
    private PredicateGroup multiValueOrGroup(String propertyPath,
                                             List<String> values) {
        PredicateGroup orGroup = new PredicateGroup();
        orGroup.setAllRequired(false); // false = OR — any value match is enough

        for (String value : values) {
            Predicate p = new Predicate(JcrPropertyPredicateEvaluator.PROPERTY);
            p.set(JcrPropertyPredicateEvaluator.PROPERTY,  propertyPath);
            p.set(JcrPropertyPredicateEvaluator.VALUE,     value);
            p.set(JcrPropertyPredicateEvaluator.OPERATION, JcrPropertyPredicateEvaluator.OP_EQUALS);
            orGroup.add(p);
        }
        return orGroup;
    }

    // ================================================================
    // Helper — creates a one-key Predicate (for simple flat predicates
    //          like "type", "nodename" that only have a single value).
    // ================================================================
    private Predicate simplePredicate(String type, String value) {
        Predicate p = new Predicate(type);
        p.set(type, value);
        return p;
    }

    // ================================================================
    // Map JCR hits → ArticleHit DTOs
    // One bad Hit never fails the whole page — caught individually.
    // ================================================================
    private List<ArticleSearchResult.ArticleHit> mapHits(SearchResult searchResult) {
        List<ArticleSearchResult.ArticleHit> results = new ArrayList<>();

        for (Hit hit : searchResult.getHits()) {
            try {
                Resource page = hit.getResource();
                if (page == null) continue;

                Resource content = page.getChild("jcr:content");
                if (content == null) continue;

                ValueMap props = content.getValueMap();

                String[] rawTags = props.get("cq:tags", String[].class);

                results.add(new ArticleSearchResult.ArticleHit(
                        page.getPath(),
                        props.get("jcr:title",   String.class),
                        props.get("authorId",    String.class),
                        props.get("publishDate", String.class),
                        props.get("category",    String.class),
                        rawTags != null ? Arrays.asList(rawTags) : Collections.emptyList(),
                        props.get("featured",    false),
                        props.get("thumbnail",   String.class)
                ));

            } catch (RepositoryException e) {
                log.warn("ArticleSearchService: skipping hit — {}", e.getMessage());
            }
        }
        return results;
    }

    // ================================================================
    // Helpers
    // ================================================================
    private ArticleSearchResult emptyResult(ArticleSearchRequest req) {
        return new ArticleSearchResult(
                Collections.emptyList(), 0L, false,
                req.getPageNumber(), req.getPageSize(), 0L);
    }

    private void logPredicates(PredicateGroup group) {
        if (log.isDebugEnabled()) {
            log.debug("ArticleSearchService query PredicateGroup: {}", group);
        }
    }
}