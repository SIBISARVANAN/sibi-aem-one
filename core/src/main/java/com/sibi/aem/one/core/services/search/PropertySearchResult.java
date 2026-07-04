package com.sibi.aem.one.core.services.search;

import java.util.List;
import java.util.Map;

/** Result envelope for a property search. */
public class PropertySearchResult {

    private final List<PropertyHit> hits;
    private final long totalMatches;
    private final boolean hasMore;
    private final Map<String, Map<String, Long>> facets; // field → value → count
    private final long queryTimeMs;

    public PropertySearchResult(List<PropertyHit> hits, long totalMatches,
                                boolean hasMore, Map<String, Map<String, Long>> facets,
                                long queryTimeMs) {
        this.hits         = hits;
        this.totalMatches = totalMatches;
        this.hasMore      = hasMore;
        this.facets       = facets;
        this.queryTimeMs  = queryTimeMs;
    }

    public List<PropertyHit> getHits()                   { return hits; }
    public long getTotalMatches()                         { return totalMatches; }
    public boolean isHasMore()                            { return hasMore; }
    public Map<String, Map<String, Long>> getFacets()    { return facets; }
    public long getQueryTimeMs()                          { return queryTimeMs; }

    // ── Inner DTOs ────────────────────────────────────────────────────────────

    public static class PropertyHit {
        private final String path;
        private final String title;
        private final String propertyType;
        private final String status;
        private final double price;
        private final String currency;
        private final boolean featured;
        private final double relevanceScore;

        public PropertyHit(String path, String title, String propertyType,
                           String status, double price, String currency,
                           boolean featured, double relevanceScore) {
            this.path           = path;
            this.title          = title;
            this.propertyType   = propertyType;
            this.status         = status;
            this.price          = price;
            this.currency       = currency;
            this.featured       = featured;
            this.relevanceScore = relevanceScore;
        }

        public String getPath()           { return path; }
        public String getTitle()          { return title; }
        public String getPropertyType()   { return propertyType; }
        public String getStatus()         { return status; }
        public double getPrice()          { return price; }
        public String getCurrency()       { return currency; }
        public boolean isFeatured()       { return featured; }
        public double getRelevanceScore() { return relevanceScore; }
    }
}
