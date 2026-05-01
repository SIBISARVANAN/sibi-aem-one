package com.sibi.aem.one.core.services.search;

import java.util.List;

/**
 * Rich result object returned by ArticleSearchService.
 * Separates the raw JCR hits from business-level metadata.
 */
public class ArticleSearchResult {

    private final List<ArticleHit> hits;
    private final long totalMatches;
    private final boolean hasMore;
    private final int pageNumber;
    private final int pageSize;
    private final long queryTimeMs;

    public ArticleSearchResult(List<ArticleHit> hits, long totalMatches,
                               boolean hasMore, int pageNumber,
                               int pageSize, long queryTimeMs) {
        this.hits         = hits;
        this.totalMatches = totalMatches;
        this.hasMore      = hasMore;
        this.pageNumber   = pageNumber;
        this.pageSize     = pageSize;
        this.queryTimeMs  = queryTimeMs;
    }

    public List<ArticleHit> getHits()    { return hits; }
    public long getTotalMatches()        { return totalMatches; }
    public boolean isHasMore()           { return hasMore; }
    public int getPageNumber()           { return pageNumber; }
    public int getPageSize()             { return pageSize; }
    public long getQueryTimeMs()         { return queryTimeMs; }

    // --- Inner DTO representing one article hit ---

    public static class ArticleHit {
        private final String path;
        private final String title;
        private final String author;
        private final String publishDate;
        private final String category;
        private final List<String> tags;
        private final boolean featured;
        private final String thumbnailPath;

        public ArticleHit(String path, String title, String author,
                          String publishDate, String category,
                          List<String> tags, boolean featured,
                          String thumbnailPath) {
            this.path          = path;
            this.title         = title;
            this.author        = author;
            this.publishDate   = publishDate;
            this.category      = category;
            this.tags          = tags;
            this.featured      = featured;
            this.thumbnailPath = thumbnailPath;
        }

        public String getPath()          { return path; }
        public String getTitle()         { return title; }
        public String getAuthor()        { return author; }
        public String getPublishDate()   { return publishDate; }
        public String getCategory()      { return category; }
        public List<String> getTags()    { return tags; }
        public boolean isFeatured()      { return featured; }
        public String getThumbnailPath() { return thumbnailPath; }
    }
}