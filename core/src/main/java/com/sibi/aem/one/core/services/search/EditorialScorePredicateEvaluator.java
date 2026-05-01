package com.sibi.aem.one.core.services.search;

import com.day.cq.search.Predicate;
import com.day.cq.search.eval.AbstractPredicateEvaluator;
import com.day.cq.search.eval.EvaluationContext;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.Row;

/**
 * ================================================================
 * EditorialScorePredicateEvaluator
 * ================================================================
 *
 * WHY this deserves a custom evaluator:
 *
 *   This predicate filters articles by a computed editorial score —
 *   a combination of THREE separate JCR properties:
 *     - featured   (boolean)    weight: +50
 *     - priority   (String)     weight: high=30, medium=20, low=10
 *     - viewCount  (Long)       weight: viewCount / 1000 (capped at 20)
 *
 *   The built-in property predicate can only compare one property
 *   against one value. It cannot:
 *     a) Read multiple properties and combine them arithmetically
 *     b) Apply conditional weights (high/medium/low → 30/20/10)
 *     c) Normalise a numeric value (viewCount / 1000)
 *     d) Compare the RESULT of that computation against a threshold
 *
 *   This is a genuine use case for a custom evaluator: the filtering
 *   rule cannot be expressed with any combination of built-in predicates.
 *
 * Usage in Map params:
 *   params.put("editorialScore.minScore", "60");
 *
 * Usage via PredicateGroup API:
 *   Predicate score = new Predicate("editorialScore");
 *   score.set("editorialScore.minScore", "60");
 *   rootGroup.add(score);
 *
 * Registration key: "predicate.name=editorialScore"
 * ================================================================
 */
@Component(
        service = com.day.cq.search.eval.PredicateEvaluator.class,
        property = {
                "predicate.name=editorialScore"
        }
)
public class EditorialScorePredicateEvaluator extends AbstractPredicateEvaluator {

    private static final Logger log =
            LoggerFactory.getLogger(EditorialScorePredicateEvaluator.class);

    static final String PARAM_MIN_SCORE = "editorialScore.minScore";
    static final int    DEFAULT_MIN     = 50;

    /**
     * Called per node. Returns true if the node's computed editorial
     * score meets or exceeds the requested minimum.
     */
    @Override
    public boolean includes(Predicate predicate, Row row,
                            EvaluationContext context) {
        int minScore = parseMinScore(predicate.get(PARAM_MIN_SCORE));

        try {
            Node node = row.getNode();
            if (node == null || !node.hasNode("jcr:content")) return false;

            Node content = node.getNode("jcr:content");
            int score    = computeScore(content);

            log.trace("editorialScore for {}: {} (min={})", node.getPath(), score, minScore);
            return score >= minScore;

        } catch (RepositoryException e) {
            log.warn("editorialScore: could not evaluate node — excluding", e);
            return false;
        }
    }

    /**
     * This predicate CANNOT be pushed to XPath/SQL2 because its logic
     * is arithmetic across multiple properties. It always runs as an
     * in-memory post-filter after Oak returns the base result set.
     *
     * Interview point: the trade-off is correctness vs. performance.
     * Use this only when the base predicates (type, path, daterange, etc.)
     * already narrow the Oak result set down sufficiently.
     */
    @Override
    public boolean canXpath(Predicate predicate, EvaluationContext context) {
        return false;
    }

    @Override
    public boolean canFilter(Predicate predicate, EvaluationContext context) {
        return true;
    }

    // -------------------------------------------------------
    // Score computation — this is the logic that makes a
    // custom evaluator justified. No built-in predicate can
    // express this multi-property weighted calculation.
    // -------------------------------------------------------
    private int computeScore(Node content) throws RepositoryException {
        int score = 0;

        // +50 if explicitly featured
        if (content.hasProperty("featured")
                && content.getProperty("featured").getBoolean()) {
            score += 50;
        }

        // +30 / +20 / +10 based on editorial priority
        if (content.hasProperty("priority")) {
            switch (content.getProperty("priority").getString()) {
                case "high":   score += 30; break;
                case "medium": score += 20; break;
                case "low":    score += 10; break;
                default:       break;
            }
        }

        // +0..20 based on view count (normalised, capped)
        if (content.hasProperty("viewCount")) {
            long views       = content.getProperty("viewCount").getLong();
            int  viewScore   = (int) Math.min(views / 1000L, 20L);
            score += viewScore;
        }

        return score;
    }

    private int parseMinScore(String param) {
        if (StringUtils.isBlank(param)) return DEFAULT_MIN;
        try {
            return Integer.parseInt(param.trim());
        } catch (NumberFormatException e) {
            return DEFAULT_MIN;
        }
    }

}