package com.sibi.aem.one.core.services.search;

import org.apache.sling.api.resource.ResourceResolver;

import java.util.List;
import java.util.Map;

/**
 * Service interface for property listing and agent search.
 *
 * <p>All methods delegate to QueryBuilder internally, which in turn uses the
 * custom Oak indexes defined in ui.apps/oak:index/. Callers never reference
 * index names directly — Oak's query planner picks the right index
 * automatically based on predicates and cost estimation.</p>
 */
public interface PropertySearchService {

    /**
     * Full property search: full-text + filters + facets + sort + pagination.
     * Uses: propertyListingLuceneIndex
     */
    PropertySearchResult searchProperties(PropertySearchRequest request,
                                          ResourceResolver resolver);

    /**
     * Quick agent lookup by agentId.
     * Uses: propertyListingPropertyIndex (synchronous, instant)
     */
    String findAgentPagePath(String agentId, ResourceResolver resolver);

    /**
     * Full-text search on agent profiles.
     * Uses: agentProfileLuceneIndex
     */
    List<AgentHit> searchAgents(String query, String specialisation,
                                ResourceResolver resolver);
}
