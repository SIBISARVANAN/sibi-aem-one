package com.sibi.aem.one.core.models;

import com.sibi.aem.one.core.services.search.AgentHit;
import com.sibi.aem.one.core.services.search.PropertySearchRequest;
import com.sibi.aem.one.core.services.search.PropertySearchResult;
import com.sibi.aem.one.core.services.search.PropertySearchService;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * Sling Model for the {@code propertysearch} component.
 *
 * <h2>Two sources of configuration</h2>
 * <ol>
 *   <li><strong>Dialog-authored config</strong> (from JCR, via {@code @ValueMapValue}) —
 *       set once by the editor: search root path, default sort, page size,
 *       which filters to show, no-results message etc.</li>
 *   <li><strong>Runtime request parameters</strong> (from the visitor's browser) —
 *       the actual search query, selected filters, current page.</li>
 * </ol>
 *
 * <p>This model reads both, merges them, executes the search via
 * {@link PropertySearchService}, and exposes clean getters to the HTL template.
 * The HTL never reads request parameters directly.</p>
 *
 * <h2>Index used</h2>
 * <p>All queries delegate to {@link PropertySearchService}, which internally
 * uses QueryBuilder. Oak's query planner automatically selects
 * {@code propertyListingLuceneIndex} for property searches and
 * {@code agentProfileLuceneIndex} for agent searches — the model never
 * references index names directly.</p>
 */
@Model(
        adaptables = SlingHttpServletRequest.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class PropertySearchModel {

    private static final Logger LOG = LoggerFactory.getLogger(PropertySearchModel.class);

    // ── Injections ────────────────────────────────────────────────────────────

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private Resource resource;

    /**
     * OSGi service injection into a Sling Model.
     * Same pattern as ProductImpl → InventoryService.
     * The model does NOT open ResourceResolvers itself — it uses the
     * request's existing resolver, which is managed by the Sling lifecycle.
     */
    @OSGiService
    private PropertySearchService propertySearchService;

    // ── Dialog-authored configuration (read from JCR via @ValueMapValue) ─────

    /** Root path the editor configured for this search instance. */
    @ValueMapValue
    private String searchRootPath;

    /** Number of results per page, authored in dialog. */
    @ValueMapValue
    private int pageSize;

    /** Default sort property, authored in dialog. */
    @ValueMapValue
    private String defaultSortProperty;

    /** Default sort order, authored in dialog. */
    @ValueMapValue
    private String defaultSortOrder;

    // ── Filter visibility flags (authored in dialog) ──────────────────────────

    @ValueMapValue
    private boolean showPropertyTypeFilter;

    @ValueMapValue
    private boolean showStatusFilter;

    @ValueMapValue
    private boolean showPriceFilter;

    @ValueMapValue
    private boolean showFeaturedFilter;

    // ── Display options (authored in dialog) ──────────────────────────────────

    @ValueMapValue
    private String noResultsMessage;

    @ValueMapValue
    private String placeholderText;

    @ValueMapValue
    private boolean showQueryTime;

    // ── Derived fields (from @PostConstruct) ─────────────────────────────────

    private PropertySearchResult searchResult;

    // Current request parameter values — exposed to HTL for form pre-fill
    private String currentQuery;
    private List<String> currentTypes;
    private List<String> currentStatuses;
    private String currentPriceMin;
    private String currentPriceMax;
    private boolean currentFeaturedOnly;
    private String currentSortProperty;
    private String currentSortOrder;
    private int currentPage;
    private boolean searchExecuted;

    // ── @PostConstruct ────────────────────────────────────────────────────────

    @PostConstruct
    protected void init() {
        applyDefaults();
        readRequestParameters();
        executeSearchIfNeeded();
    }

    /**
     * Apply dialog config defaults, falling back to hardcoded sensible values
     * if the editor left fields empty.
     */
    private void applyDefaults() {
        if (StringUtils.isBlank(searchRootPath)) {
            searchRootPath = "/content/sibi-aem-one";
        }
        if (pageSize <= 0) {
            pageSize = 12;
        }
        if (StringUtils.isBlank(defaultSortProperty)) {
            defaultSortProperty = "jcr:content/price";
        }
        if (StringUtils.isBlank(defaultSortOrder)) {
            defaultSortOrder = "asc";
        }
        if (StringUtils.isBlank(noResultsMessage)) {
            noResultsMessage = "No properties found matching your search.";
        }
        if (StringUtils.isBlank(placeholderText)) {
            placeholderText = "Search properties...";
        }
    }

    /**
     * Reads visitor-supplied request parameters. These override (or combine with)
     * the dialog defaults at runtime.
     *
     * Dialog config  = "what the editor set up"
     * Request params = "what the visitor is searching for right now"
     */
    private void readRequestParameters() {
        currentQuery       = StringUtils.defaultIfBlank(request.getParameter("q"), "");
        currentTypes       = splitParam(request.getParameter("types"));
        currentStatuses    = splitParam(request.getParameter("statuses"));
        currentPriceMin    = request.getParameter("priceMin");
        currentPriceMax    = request.getParameter("priceMax");
        currentFeaturedOnly = "true".equals(request.getParameter("featured"));
        currentPage        = parseIntOrDefault(request.getParameter("page"), 0);

        // Sort: request param overrides dialog default
        currentSortProperty = StringUtils.defaultIfBlank(
                request.getParameter("sortBy"), defaultSortProperty);
        currentSortOrder = StringUtils.defaultIfBlank(
                request.getParameter("sortOrder"), defaultSortOrder);
    }

    /**
     * Only runs the search when at least one search signal is present.
     * Prevents an expensive query on initial page load when no search
     * has been entered yet.
     */
    private void executeSearchIfNeeded() {
        boolean hasSearchSignal = StringUtils.isNotBlank(currentQuery)
                || !currentTypes.isEmpty()
                || !currentStatuses.isEmpty()
                || StringUtils.isNotBlank(currentPriceMin)
                || StringUtils.isNotBlank(currentPriceMax)
                || currentFeaturedOnly;

        if (!hasSearchSignal) {
            searchExecuted = false;
            return;
        }

        try {
            PropertySearchRequest searchRequest = buildSearchRequest();
            searchResult = propertySearchService.searchProperties(
                    searchRequest, request.getResourceResolver());
            searchExecuted = true;
            LOG.debug("Property search executed — query='{}', total={}, time={}ms",
                    currentQuery, searchResult.getTotalMatches(), searchResult.getQueryTimeMs());
        } catch (Exception e) {
            LOG.error("Property search failed for query '{}': {}", currentQuery, e.getMessage(), e);
            searchExecuted = false;
        }
    }

    /**
     * Builds the {@link PropertySearchRequest} DTO by combining:
     * - Dialog-authored config (searchRootPath, pageSize)
     * - Runtime request parameters (query, filters, sort, page)
     */
    private PropertySearchRequest buildSearchRequest() {
        return PropertySearchRequest.builder()
                .fullText(currentQuery)
                .propertyTypes(currentTypes)
                .statuses(currentStatuses)
                .priceMin(parseDouble(currentPriceMin))
                .priceMax(parseDouble(currentPriceMax))
                .featuredOnly(currentFeaturedOnly)
                .sortBy(currentSortProperty, currentSortOrder)
                .page(currentPage, pageSize)
                .build();
    }

    // ── Public getters exposed to HTL ─────────────────────────────────────────

    /** @return search result hits, empty list if no search was executed */
    public List<PropertySearchResult.PropertyHit> getHits() {
        if (searchResult == null) return Collections.emptyList();
        return searchResult.getHits();
    }

    /**
     * @return propertyType facet counts: value → count.
     *         E.g. {"villa":45, "apartment":32}
     *         Sourced directly from the Lucene facet cache — no extra JCR reads.
     */
    public Map<String, Long> getPropertyTypeFacets() {
        if (searchResult == null) return Collections.emptyMap();
        return searchResult.getFacets().getOrDefault(
                "jcr:content/propertyType", Collections.emptyMap());
    }

    /** @return status facet counts: value → count */
    public Map<String, Long> getStatusFacets() {
        if (searchResult == null) return Collections.emptyMap();
        return searchResult.getFacets().getOrDefault(
                "jcr:content/status", Collections.emptyMap());
    }

    /** @return estimated total matches (from p.guessTotal) */
    public long getTotalMatches() {
        return searchResult != null ? searchResult.getTotalMatches() : 0L;
    }

    /** @return true if more pages of results exist beyond the current page */
    public boolean isHasMore() {
        return searchResult != null && searchResult.isHasMore();
    }

    /** @return query execution time in milliseconds */
    public long getQueryTimeMs() {
        return searchResult != null ? searchResult.getQueryTimeMs() : 0L;
    }

    /** @return true if a search was attempted */
    public boolean isSearchExecuted() {
        return searchExecuted;
    }

    /** @return true if search was run but returned zero results */
    public boolean isNoResults() {
        return searchExecuted && getTotalMatches() == 0;
    }

    /** @return zero-based index of the previous page, or -1 if on first page */
    public int getPreviousPage() {
        return currentPage > 0 ? currentPage - 1 : -1;
    }

    /** @return zero-based index of the next page, or -1 if no more results */
    public int getNextPage() {
        return isHasMore() ? currentPage + 1 : -1;
    }

    // ── Request param getters — for pre-filling form fields in HTL ────────────

    public String getCurrentQuery()          { return currentQuery; }
    public List<String> getCurrentTypes()    { return currentTypes; }
    public List<String> getCurrentStatuses() { return currentStatuses; }
    public String getCurrentPriceMin()       { return StringUtils.defaultIfBlank(currentPriceMin, ""); }
    public String getCurrentPriceMax()       { return StringUtils.defaultIfBlank(currentPriceMax, ""); }
    public boolean isCurrentFeaturedOnly()   { return currentFeaturedOnly; }
    public String getCurrentSortProperty()   { return currentSortProperty; }
    public String getCurrentSortOrder()      { return currentSortOrder; }
    public int getCurrentPage()              { return currentPage; }

    // ── Dialog config getters — for conditional rendering in HTL ─────────────

    public boolean isShowPropertyTypeFilter() { return showPropertyTypeFilter; }
    public boolean isShowStatusFilter()       { return showStatusFilter; }
    public boolean isShowPriceFilter()        { return showPriceFilter; }
    public boolean isShowFeaturedFilter()     { return showFeaturedFilter; }
    public boolean isShowQueryTime()          { return showQueryTime; }
    public String getNoResultsMessage()       { return noResultsMessage; }
    public String getPlaceholderText()        { return placeholderText; }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<String> splitParam(String param) {
        if (StringUtils.isBlank(param)) return Collections.emptyList();
        return Arrays.asList(param.trim().split(","));
    }

    private Double parseDouble(String val) {
        try { return StringUtils.isNotBlank(val) ? Double.parseDouble(val.trim()) : null; }
        catch (NumberFormatException e) { return null; }
    }

    private int parseIntOrDefault(String val, int def) {
        try { return StringUtils.isNotBlank(val) ? Integer.parseInt(val.trim()) : def; }
        catch (NumberFormatException e) { return def; }
    }
}
