package com.sibi.aem.one.core.services.search;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
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
 * Implementation of {@link PropertySearchService}.
 *
 * <h2>Index Usage Map</h2>
 * <ul>
 *   <li>{@link #searchProperties} → Oak auto-selects {@code propertyListingLuceneIndex}
 *       because it's the only index covering /content/sibi-aem-one with the
 *       combination of full-text + propertyType + price range predicates.</li>
 *   <li>{@link #findAgentPagePath} → Oak selects {@code propertyListingPropertyIndex}
 *       (type=property, synchronous) for the single agentId equality lookup.</li>
 *   <li>{@link #searchAgents} → Oak selects {@code agentProfileLuceneIndex}
 *       because the path /content/sibi-aem-one/en/agents matches its includedPaths
 *       and no other index covers that path with full-text capabilities.</li>
 * </ul>
 *
 * <h2>How Oak picks the right index without being told</h2>
 * <p>You never reference an index by name in QueryBuilder code. Oak's query
 * planner scores every available index against your query's predicates using
 * cost estimation (based on stored index statistics). The lowest-cost index wins.
 * You verify the choice with {@code p.explain=true} in the QueryBuilder JSON URL.</p>
 */
@Component(service = PropertySearchService.class)
public class PropertySearchServiceImpl implements PropertySearchService {

    private static final Logger LOG = LoggerFactory.getLogger(PropertySearchServiceImpl.class);

    private static final String PROPERTY_LISTING_ROOT = "/content/sibi-aem-one";
    private static final String AGENT_ROOT            = "/content/sibi-aem-one/en/agents";
    private static final String PROPERTY_RESOURCE_TYPE =
            "sibi-aem-one/components/content/propertylisting";
    private static final String AGENT_RESOURCE_TYPE =
            "sibi-aem-one/components/content/agentprofile";

    @Reference
    private QueryBuilder queryBuilder;

    // ══════════════════════════════════════════════════════════════════════════
    // METHOD 1 — Full property search using propertyListingLuceneIndex
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public PropertySearchResult searchProperties(PropertySearchRequest req,
                                                  ResourceResolver resolver) {
        long start = System.currentTimeMillis();
        Map<String, String> params = buildPropertySearchParams(req);

        Session session = resolver.adaptTo(Session.class);
        Query query = queryBuilder.createQuery(PredicateGroup.create(params), session);

        SearchResult result = query.getResult();

        try {
            List<PropertySearchResult.PropertyHit> hits = mapPropertyHits(result);
            Map<String, Map<String, Long>> facets = extractFacets(result);

            long total = result.getTotalMatches();
            boolean hasMore = (long) req.getPageNumber() * req.getPageSize()
                              + hits.size() < total;

            return new PropertySearchResult(hits, total, hasMore, facets,
                    System.currentTimeMillis() - start);
        } catch (RepositoryException e) {
            LOG.error("Error reading property search results", e);
            return new PropertySearchResult(Collections.emptyList(), 0, false,
                    Collections.emptyMap(), System.currentTimeMillis() - start);
        }
        // NOTE ON QUERY CLEANUP:
        // QueryBuilder's Query does not implement Closeable/AutoCloseable in AEM 6.5.
        // The underlying JCR QueryResult session is released automatically when the
        // ResourceResolver passed into this method is closed by the CALLER.
        // Always ensure the caller closes their ResourceResolver in a finally block.
        // Never open a ResourceResolver inside this method just to run a query —
        // use the caller's resolver so session lifecycle stays with the caller.
    }

    /**
     * Builds the flat predicate map for the QueryBuilder.
     *
     * <h3>How Oak picks propertyListingLuceneIndex for this query</h3>
     * <p>The query has: type=cq:Page, path=/content/sibi-aem-one, a fulltext
     * predicate, a property predicate on jcr:content/propertyType, and a
     * daterange/numeric range on price. The query planner scores all available
     * indexes. {@code propertyListingLuceneIndex} is the only one that:
     * (1) covers /content/sibi-aem-one (includedPaths), (2) has propertyType
     * with propertyIndex=true, and (3) has price with ordered=true. Its cost
     * is therefore much lower than the catch-all default lucene index or any
     * property index that lacks fulltext capability.</p>
     */
    private Map<String, String> buildPropertySearchParams(PropertySearchRequest req) {
        Map<String, String> p = new LinkedHashMap<>();

        // ── Node type — uses Oak's nodetype index first to narrow the candidate set
        p.put("type", "cq:Page");

        // ── Path restriction — honoured because evaluatePathRestrictions=true
        p.put("path", PROPERTY_LISTING_ROOT);
        p.put("path.exact", "false"); // search all descendants

        // ── Resource type filter — ensures we only get property listing pages,
        //    not blog pages or other cq:Page subtypes under the same path
        p.put("1_property", "jcr:content/sling:resourceType");
        p.put("1_property.value", PROPERTY_RESOURCE_TYPE);

        // ── Full-text search — uses the analyzed, nodeScopeIndex=true fields
        //    (jcr:title + shortDescription) via the Lucene inverted index.
        //    The Analyzer runs on the query term too: "Villas" → stem → "villa"
        //    → matches documents indexed with "villa", "villas", "villagio" etc.
        if (StringUtils.isNotBlank(req.getFullTextQuery())) {
            p.put("fulltext", req.getFullTextQuery());
            p.put("fulltext.relPath", "jcr:content"); // scope to page content only
        }

        // ── propertyType OR filter — multiple values in one predicate
        //    If ["villa","penthouse"] → matches pages where propertyType is EITHER
        if (req.getPropertyTypes() != null && !req.getPropertyTypes().isEmpty()) {
            p.put("2_property", "jcr:content/propertyType");
            p.put("2_property.operation", "equals");
            for (int i = 0; i < req.getPropertyTypes().size(); i++) {
                p.put("2_property." + (i + 1) + "_value", req.getPropertyTypes().get(i));
            }
            p.put("2_property.and", "false"); // OR logic: any of the values match
        }

        // ── status OR filter
        if (req.getStatuses() != null && !req.getStatuses().isEmpty()) {
            p.put("3_property", "jcr:content/status");
            for (int i = 0; i < req.getStatuses().size(); i++) {
                p.put("3_property." + (i + 1) + "_value", req.getStatuses().get(i));
            }
            p.put("3_property.and", "false");
        }

        // ── Price range — uses the ordered=true Double field in the Lucene index.
        //    Without type="Double" in the index, this would sort/range incorrectly
        //    (string comparison: "1000000" < "500000" because "1" < "5").
        int predicateCounter = 4;
        if (req.getPriceMin() != null || req.getPriceMax() != null) {
            String prefix = predicateCounter + "_";
            p.put(prefix + "rangeproperty", "jcr:content/price");
            p.put(prefix + "rangeproperty.property.operation", "exists");
            if (req.getPriceMin() != null) {
                p.put(prefix + "rangeproperty.lowerBound",
                        String.valueOf(req.getPriceMin()));
                p.put(prefix + "rangeproperty.lowerOperation", ">=");
            }
            if (req.getPriceMax() != null) {
                p.put(prefix + "rangeproperty.upperBound",
                        String.valueOf(req.getPriceMax()));
                p.put(prefix + "rangeproperty.upperOperation", "<=");
            }
            predicateCounter++;
        }

        // ── Amenity tags — AND filter: ALL specified tags must be present.
        //    QueryBuilder's "tagid" predicate handles multi-value String[] properties.
        if (req.getAmenityTags() != null && !req.getAmenityTags().isEmpty()) {
            for (int i = 0; i < req.getAmenityTags().size(); i++) {
                String prefix = (predicateCounter + i) + "_";
                p.put(prefix + "property", "jcr:content/amenityTags");
                p.put(prefix + "property.value", req.getAmenityTags().get(i));
                // Default is AND — each predicate must match independently
            }
            predicateCounter += req.getAmenityTags().size();
        }

        // ── Featured filter
        if (Boolean.TRUE.equals(req.getFeaturedOnly())) {
            p.put(predicateCounter + "_property", "jcr:content/featured");
            p.put(predicateCounter + "_property.value", "true");
            predicateCounter++;
        }

        // ── Sorting — uses the ordered=true field in the Lucene index.
        //    If the sort field doesn't have ordered=true, Oak falls back to
        //    loading every result node from JCR to read the property — O(n) reads.
        //    With ordered=true, Lucene stores DocValues — sort is O(1) in the index.
        p.put("orderby", "@" + req.getSortProperty());
        p.put("orderby.sort", req.getSortOrder());

        // ── Pagination
        p.put("p.limit", String.valueOf(req.getPageSize()));
        p.put("p.offset", String.valueOf(
                (long) req.getPageNumber() * req.getPageSize()));

        // ── p.guessTotal: CRITICAL for performance.
        //    Without this, QueryBuilder traverses EVERY matching node to compute
        //    an exact count — catastrophic on large repos.
        //    With this, counting stops at the estimate value and approximates.
        //    Set it to pageSize * 10 as a reasonable upper estimate.
        p.put("p.guessTotal", String.valueOf(req.getPageSize() * 10));

        // ── Facets — requests aggregate counts for propertyType and status.
        //    Requires facets=true on these fields in the index definition.
        //    Without the index flag, facet queries load all result nodes from JCR.
        p.put("facetextract.1_property", "jcr:content/propertyType");
        p.put("facetextract.1_property.count", "20");
        p.put("facetextract.2_property", "jcr:content/status");
        p.put("facetextract.2_property.count", "10");

        return p;
    }

    private List<PropertySearchResult.PropertyHit> mapPropertyHits(SearchResult result)
            throws RepositoryException {

        List<PropertySearchResult.PropertyHit> hits = new ArrayList<>();
        for (Hit hit : result.getHits()) {
            try {
                Resource resource = hit.getResource();
                Resource content  = resource.getChild("jcr:content");
                if (content == null) continue;

                ValueMap vm = content.getValueMap();
                hits.add(new PropertySearchResult.PropertyHit(
                        resource.getPath(),
                        vm.get("jcr:title", String.class),
                        vm.get("propertyType", String.class),
                        vm.get("status", String.class),
                        vm.get("price", 0d),
                        vm.get("currency", "GBP"),
                        vm.get("featured", false),
                        hit.getScore()
                ));
            } catch (RepositoryException e) {
                LOG.warn("Error reading hit {}: {}", hit, e.getMessage());
            }
        }
        return hits;
    }

    private Map<String, Map<String, Long>> extractFacets(SearchResult result) {
        Map<String, Map<String, Long>> facets = new LinkedHashMap<>();
        try {
            // getFacets() returns data from the Lucene index's facet cache —
            // no additional JCR reads needed. Only works when facets=true in index.
            // getFacets() returns Map<String, Facet> — one Facet per requested field.
            // Facet.getBuckets() returns the List<Bucket> of value→count pairs.
            Map<String, com.day.cq.search.facets.Facet> raw = result.getFacets();
            if (raw == null) return facets;
            raw.forEach((field, facet) -> {
                Map<String, Long> counts = new LinkedHashMap<>();
                if (facet != null && facet.getBuckets() != null) {
                    facet.getBuckets().forEach(b ->
                        counts.put(b.getValue(), (long) b.getCount()));
                }
                facets.put(field, counts);
            });
        } catch (Exception e) {
            LOG.warn("Error extracting facets: {}", e.getMessage());
        }
        return facets;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // METHOD 2 — Agent lookup using propertyListingPropertyIndex (synchronous)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Finds an agent's page path given their agentId.
     *
     * <h3>Why this uses the property index (not the Lucene index)</h3>
     * <p>This is a single equality lookup called on EVERY property listing
     * page render to resolve the agent link. It must be:
     * <ul>
     *   <li><strong>Synchronous</strong> — agent profile updates must be
     *       immediately visible, no 5-second async lag acceptable here.</li>
     *   <li><strong>Lightweight</strong> — no full-text, no facets, no sort.
     *       The property index is a simple B-tree — faster than Lucene for
     *       single-property equality lookups.</li>
     * </ul>
     * Oak selects {@code propertyListingPropertyIndex} because it is scoped
     * to /content/sibi-aem-one/en/agents, covers agentId, is type=property
     * (lower overhead than lucene for exact lookups), and updates synchronously.</p>
     */
    @Override
    public String findAgentPagePath(String agentId, ResourceResolver resolver) {
        if (StringUtils.isBlank(agentId)) return null;

        Map<String, String> params = new LinkedHashMap<>();
        params.put("type", "cq:Page");
        params.put("path", AGENT_ROOT);
        params.put("1_property", "jcr:content/agentId");
        params.put("1_property.value", agentId);
        params.put("p.limit", "1");        // we only need one result
        params.put("p.guessTotal", "1");   // stop counting after 1

        Session session = resolver.adaptTo(Session.class);
        Query query = queryBuilder.createQuery(PredicateGroup.create(params), session);
        SearchResult result = query.getResult();

        try {
            List<Hit> hits = result.getHits();
            return hits.isEmpty() ? null : hits.get(0).getPath();
        } catch (RepositoryException e) {
            LOG.error("Error finding agent page for id {}: {}", agentId, e.getMessage(), e);
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // METHOD 3 — Agent full-text search using agentProfileLuceneIndex
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Full-text search on agent profiles.
     *
     * <h3>Why this uses agentProfileLuceneIndex instead of propertyListingLuceneIndex</h3>
     * <p>The agentProfileLuceneIndex is scoped to /content/sibi-aem-one/en/agents
     * and indexes agentName with boost=3.0 — a name match ranks much higher than
     * a bio match. The propertyListingLuceneIndex doesn't cover the agents path
     * (includedPaths excludes it), so Oak would fall back to traversal. By having
     * a separate index for agents, we get a faster, more relevant result set.</p>
     */
    @Override
    public List<AgentHit> searchAgents(String fullTextQuery, String specialisation,
                                        ResourceResolver resolver) {

        Map<String, String> params = new LinkedHashMap<>();
        params.put("type", "cq:Page");
        params.put("path", AGENT_ROOT);
        params.put("1_property", "jcr:content/sling:resourceType");
        params.put("1_property.value", AGENT_RESOURCE_TYPE);

        if (StringUtils.isNotBlank(fullTextQuery)) {
            params.put("fulltext", fullTextQuery);
            // No relPath restriction — searches agentName AND bio (both are
            // nodeScopeIndex=true in agentProfileLuceneIndex)
        }

        if (StringUtils.isNotBlank(specialisation)) {
            params.put("2_property", "jcr:content/specialisation");
            params.put("2_property.value", specialisation);
        }

        // No explicit orderby — results ordered by @jcr:score (relevance).
        // agentName has boost=3.0 so name matches rank above bio matches.
        params.put("p.limit", "20");
        params.put("p.guessTotal", "50");

        // Facet on specialisation for the agent search sidebar
        params.put("facetextract.1_property", "jcr:content/specialisation");
        params.put("facetextract.1_property.count", "10");

        Session session = resolver.adaptTo(Session.class);
        Query query = queryBuilder.createQuery(PredicateGroup.create(params), session);

        List<AgentHit> hits = new ArrayList<>();
        try {
            for (Hit hit : query.getResult().getHits()) {
                Resource content = hit.getResource().getChild("jcr:content");
                if (content == null) continue;
                ValueMap vm = content.getValueMap();
                hits.add(new AgentHit(
                        hit.getPath(),
                        vm.get("agentName", String.class),
                        vm.get("agentId", String.class),
                        vm.get("specialisation", String.class),
                        hit.getScore()
                ));
            }
        } catch (RepositoryException e) {
            LOG.error("Error searching agents: {}", e.getMessage(), e);
        }
        return hits;
    }
}
