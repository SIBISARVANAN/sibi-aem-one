package com.sibi.aem.one.core.services.search;

import com.day.cq.search.Predicate;
import com.day.cq.search.eval.AbstractPredicateEvaluator;
import com.day.cq.search.eval.EvaluationContext;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.Row;

/**
 * Custom PredicateEvaluator that filters articles by their "featured" flag.
 *
 * This evaluator cannot be pushed down to XPath (canXpath = false) because
 * "featured" is a boolean property that may not be indexed. It runs as a
 * post-filter on the result set returned by Oak.
 *
 * Usage in Map params:
 *   params.put("featuredArticle",          "true");
 *   params.put("featuredArticle.property", "jcr:content/featured");  // optional override
 *
 * Registration: the OSGi property "predicate.name" tells QueryBuilder which
 * predicate name routes to this evaluator. PredicateEvaluator.NAME does NOT
 * exist as a compile-time constant — the correct key is the plain string
 * "predicate.name" as defined by the QueryBuilder OSGi service contract.
 */
@Component(
        service = com.day.cq.search.eval.PredicateEvaluator.class,
        property = {
                "predicate.name=featuredArticle"
        }
)
public class FeaturedArticlePredicateEvaluator extends AbstractPredicateEvaluator {

    private static final Logger log =
            LoggerFactory.getLogger(FeaturedArticlePredicateEvaluator.class);

    /** Predicate key: "true" means only include featured articles */
    static final String PARAM_FEATURED = "featuredArticle";

    /** Optional override for the JCR property path holding the featured flag */
    static final String PARAM_PROPERTY = "property";

    static final String DEFAULT_FEATURED_PROPERTY = "jcr:content/featured";

    /**
     * Called by QueryBuilder for every node in the Oak result set.
     * Return true  → include this node in the final result.
     * Return false → exclude it (post-filter).
     */
    @Override
    public boolean includes(Predicate predicate, Row row, EvaluationContext context) {
        String featuredParam = predicate.get(PARAM_FEATURED);

        // If caller did not set featured=true, skip filtering
        if (!"true".equalsIgnoreCase(featuredParam)) {
            return true;
        }

        String propertyPath = predicate.get(PARAM_PROPERTY, DEFAULT_FEATURED_PROPERTY);

        try {
            Node node = row.getNode();
            if (node == null) return false;

            // Walk the relative property path (e.g. "jcr:content/featured")
            String[] segments   = propertyPath.split("/");
            String propName     = segments[segments.length - 1];
            Node   targetNode   = node;

            for (int i = 0; i < segments.length - 1; i++) {
                if (!targetNode.hasNode(segments[i])) return false;
                targetNode = targetNode.getNode(segments[i]);
            }

            if (!targetNode.hasProperty(propName)) return false;

            return targetNode.getProperty(propName).getBoolean();

        } catch (RepositoryException e) {
            log.warn("FeaturedArticlePredicateEvaluator: could not read property '{}' — excluding node",
                    propertyPath, e);
            return false;
        }
    }

    /**
     * Returning false means this predicate CANNOT be converted to an XPath/SQL2
     * clause and will run as an in-memory post-filter after Oak executes the query.
     *
     * Return true only when you can provide an accurate XPath fragment via
     * getXPathExpression() — otherwise QueryBuilder will generate wrong SQL.
     */
    @Override
    public boolean canXpath(Predicate predicate, EvaluationContext context) {
        return false;
    }

    /**
     * Returning false means this predicate also cannot be used as a JCR filter
     * (javax.jcr.query.Row-level filter that runs inside the JCR layer).
     * Combined with canXpath=false, the includes() method above is the sole
     * filter mechanism.
     */
    @Override
    public boolean canFilter(Predicate predicate, EvaluationContext context) {
        return true; // allows includes() to be called as a post-filter
    }
}