package com.sibi.aem.one.core.services.search;

import java.util.List;

/**
 * Encapsulates all search criteria for article queries.
 * Uses the Builder pattern — callers chain only what they need,
 * everything else falls back to a sensible default.
 */
public class ArticleSearchRequest {

    // --- Path / scope ---
    private String rootPath        = "/content/sibi-aem-one/en";
    private String excludePath;                              // path to exclude (e.g. /drafts)
    private String nodeNamePattern;                          // glob on node name, e.g. "2024-*"

    // --- Content filters ---
    private List<String> categories;
    private List<String> authors;
    private List<String> tags;
    private List<String> templates;                          // cq:template values (OR)
    private String       fullTextQuery;
    private boolean      featuredOnly    = false;
    private boolean      excludeArchived = true;

    // --- Date range ---
    private String publishedAfter;                           // ISO-8601
    private String publishedBefore;

    // --- Pagination ---
    private int pageNumber = 0;
    private int pageSize   = 10;

    // --- Ordering ---
    private String sortProperty = "jcr:content/publishDate";
    private String sortOrder    = "desc";

    private ArticleSearchRequest() {}

    public static Builder builder() { return new Builder(); }

    // --- Getters ---
    public String       getRootPath()        { return rootPath; }
    public String       getExcludePath()     { return excludePath; }
    public String       getNodeNamePattern() { return nodeNamePattern; }
    public List<String> getCategories()      { return categories; }
    public List<String> getAuthors()         { return authors; }
    public List<String> getTags()            { return tags; }
    public List<String> getTemplates()       { return templates; }
    public String       getFullTextQuery()   { return fullTextQuery; }
    public boolean      isFeaturedOnly()     { return featuredOnly; }
    public boolean      isExcludeArchived()  { return excludeArchived; }
    public String       getPublishedAfter()  { return publishedAfter; }
    public String       getPublishedBefore() { return publishedBefore; }
    public int          getPageNumber()      { return pageNumber; }
    public int          getPageSize()        { return pageSize; }
    public String       getSortProperty()    { return sortProperty; }
    public String       getSortOrder()       { return sortOrder; }

    // --- Builder ---
    public static class Builder {
        private final ArticleSearchRequest req = new ArticleSearchRequest();

        public Builder rootPath(String v)               { req.rootPath = v;        return this; }
        public Builder excludePath(String v)            { req.excludePath = v;      return this; }
        public Builder nodeNamePattern(String v)        { req.nodeNamePattern = v;  return this; }
        public Builder categories(List<String> v)       { req.categories = v;       return this; }
        public Builder authors(List<String> v)          { req.authors = v;          return this; }
        public Builder tags(List<String> v)             { req.tags = v;             return this; }
        public Builder templates(List<String> v)        { req.templates = v;        return this; }
        public Builder fullTextQuery(String v)          { req.fullTextQuery = v;    return this; }
        public Builder featuredOnly(boolean v)          { req.featuredOnly = v;     return this; }
        public Builder excludeArchived(boolean v)       { req.excludeArchived = v;  return this; }
        public Builder publishedAfter(String v)         { req.publishedAfter = v;   return this; }
        public Builder publishedBefore(String v)        { req.publishedBefore = v;  return this; }
        public Builder page(int num, int size)          { req.pageNumber = num; req.pageSize = size; return this; }
        public Builder sortBy(String prop, String order){ req.sortProperty = prop; req.sortOrder = order; return this; }

        public ArticleSearchRequest build() { return req; }
    }
}