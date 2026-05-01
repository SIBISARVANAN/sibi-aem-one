package com.sibi.aem.one.core.services.search;

import org.apache.sling.api.resource.ResourceResolver;

/**
 * Service contract for searching news articles in the JCR.
 * Implementations use the AEM QueryBuilder API under the hood.
 */
public interface ArticleSearchService {

    /**
     * Execute a structured article search.
     *
     * @param request  — fully-built ArticleSearchRequest from the caller
     * @param resolver — caller's ResourceResolver (session must remain open during the call)
     * @return         — ArticleSearchResult containing hits and pagination metadata
     */
    ArticleSearchResult search(ArticleSearchRequest request, ResourceResolver resolver);
}