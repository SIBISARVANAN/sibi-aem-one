package com.sibi.aem.one.core.services.search;

import java.util.List;

/**
 * Immutable request DTO for property search. Use the nested {@link Builder}.
 */
public class PropertySearchRequest {

    private final String fullTextQuery;
    private final List<String> propertyTypes;   // OR filter
    private final List<String> statuses;         // OR filter
    private final Double priceMin;
    private final Double priceMax;
    private final List<String> amenityTags;      // AND filter — must have ALL tags
    private final Boolean featuredOnly;
    private final String sortProperty;           // e.g. "jcr:content/price"
    private final String sortOrder;              // "asc" or "desc"
    private final int pageNumber;
    private final int pageSize;

    private PropertySearchRequest(Builder b) {
        this.fullTextQuery  = b.fullTextQuery;
        this.propertyTypes  = b.propertyTypes;
        this.statuses       = b.statuses;
        this.priceMin       = b.priceMin;
        this.priceMax       = b.priceMax;
        this.amenityTags    = b.amenityTags;
        this.featuredOnly   = b.featuredOnly;
        this.sortProperty   = b.sortProperty != null ? b.sortProperty : "@jcr:content/price";
        this.sortOrder      = b.sortOrder    != null ? b.sortOrder    : "asc";
        this.pageNumber     = b.pageNumber;
        this.pageSize       = b.pageSize > 0 ? b.pageSize : 12;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String fullTextQuery;
        private List<String> propertyTypes;
        private List<String> statuses;
        private Double priceMin;
        private Double priceMax;
        private List<String> amenityTags;
        private Boolean featuredOnly;
        private String sortProperty;
        private String sortOrder;
        private int pageNumber = 0;
        private int pageSize   = 12;

        public Builder fullText(String q)                  { fullTextQuery = q;      return this; }
        public Builder propertyTypes(List<String> types)   { propertyTypes = types;  return this; }
        public Builder statuses(List<String> s)            { statuses = s;           return this; }
        public Builder priceMin(Double min)                { priceMin = min;         return this; }
        public Builder priceMax(Double max)                { priceMax = max;         return this; }
        public Builder amenityTags(List<String> tags)      { amenityTags = tags;     return this; }
        public Builder featuredOnly(Boolean f)             { featuredOnly = f;       return this; }
        public Builder sortBy(String prop, String order)   { sortProperty = prop; sortOrder = order; return this; }
        public Builder page(int num, int size)             { pageNumber = num; pageSize = size; return this; }
        public PropertySearchRequest build()               { return new PropertySearchRequest(this); }
    }

    public String getFullTextQuery()     { return fullTextQuery; }
    public List<String> getPropertyTypes() { return propertyTypes; }
    public List<String> getStatuses()    { return statuses; }
    public Double getPriceMin()          { return priceMin; }
    public Double getPriceMax()          { return priceMax; }
    public List<String> getAmenityTags() { return amenityTags; }
    public Boolean getFeaturedOnly()     { return featuredOnly; }
    public String getSortProperty()      { return sortProperty; }
    public String getSortOrder()         { return sortOrder; }
    public int getPageNumber()           { return pageNumber; }
    public int getPageSize()             { return pageSize; }
}
