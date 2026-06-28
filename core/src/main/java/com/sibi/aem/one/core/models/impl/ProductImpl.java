package com.sibi.aem.one.core.models.impl;

import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.sibi.aem.one.core.models.Product;
import com.sibi.aem.one.core.models.ProductSummary;
import com.sibi.aem.one.core.models.ProductVariant;
import com.sibi.aem.one.core.services.InventoryService;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.ExporterOption;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Named;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link Product} Sling Model.
 *
 * <h2>Real-World Scenario</h2>
 * <p>A product detail page component. The editor fills in title, SKU, price,
 * currency, a featured flag, a category tag, and a multifield of variants.
 * At render time this model reads JCR properties, fetches live stock from an
 * inventory microservice, resolves the category tag via TagManager, and
 * exposes a JSON summary via the Jackson exporter.</p>
 *
 * <h2>Patterns Demonstrated</h2>
 * <ul>
 *   <li>{@code @ChildResource} — multifield → {@code List<Resource>} → adapted to {@link ProductVariant}</li>
 *   <li>Custom Jackson serializer via {@code @JsonSerialize} on {@link ProductSummary}</li>
 *   <li>TagManager API for resolving a stored tag ID to a localised name</li>
 *   <li>{@code @OSGiService} injection directly into a Sling Model</li>
 *   <li>Derived fields computed once in {@code @PostConstruct}, not in getters</li>
 * </ul>
 */
@Model(
        adaptables  = { Resource.class, SlingHttpServletRequest.class },
        adapters    = Product.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL,
        resourceType = ProductImpl.RESOURCE_TYPE
)
@Exporter(name = "jackson", selector = "model", extensions = "json", options = {
        @ExporterOption(name = "SerializationFeature.WRAP_ROOT_VALUE", value = "true"),
        @ExporterOption(name = "MapperFeature.SORT_PROPERTIES_ALPHABETICALLY", value = "true")
})
@JsonRootName("productSummary")
public class ProductImpl implements Product {

    private static final Logger LOG = LoggerFactory.getLogger(ProductImpl.class);
    static final String RESOURCE_TYPE = "sibi-aem-one/components/content/product";

    @ValueMapValue @Named("jcr:title") private String title;
    @ValueMapValue private String sku;
    @ValueMapValue private Double price;
    @ValueMapValue private String currency;
    @ValueMapValue private boolean featured;

    /** Single tag-path property (not cq:tags multi-value), resolved via TagManager below. */
    @ValueMapValue private String categoryTagId;

    /**
     * Injects the child node named "variants" as a {@code List<Resource>} — each
     * child node under {@code jcr:content/variants/} becomes one {@code Resource}.
     * Adapted to {@link ProductVariant} in {@code init()}. This is the
     * {@code @ChildResource} pattern missing from the original codebase.
     */
    @ChildResource(name = "variants")
    private List<Resource> variantResources;

    @Self private Resource currentResource;

    /** Only populated when adapting from a request; null-safe due to OPTIONAL strategy. */
    @Self private SlingHttpServletRequest request;

    /**
     * OSGi service injected directly into a Sling Model via {@code @OSGiService}.
     * Called once in {@code @PostConstruct} — never from a getter, or every HTL
     * access to stockCount would trigger a new HTTP call.
     */
    @OSGiService private InventoryService inventoryService;

    private String formattedPrice;
    private String categoryName;
    private int stockCount = -1;
    private List<ProductVariant> variants;
    private ProductSummary summary;

    @PostConstruct
    protected void init() {
        buildFormattedPrice();
        resolveCategoryName();
        fetchStockCount();
        adaptVariants();
        buildSummary();
    }

    private void buildFormattedPrice() {
        if (price == null) { formattedPrice = "Price unavailable"; return; }
        String symbol = getCurrencySymbol(StringUtils.defaultIfBlank(currency, "GBP"));
        formattedPrice = String.format("%s%.2f", symbol, price);
    }

    private String getCurrencySymbol(String isoCode) {
        switch (isoCode.toUpperCase()) {
            case "GBP": return "£";
            case "EUR": return "€";
            case "INR": return "₹";
            default:    return "$";
        }
    }

    /**
     * Resolves {@code categoryTagId} to a human-readable name via {@link TagManager}.
     * {@code tag.getTitle(locale)} never returns null — it falls back to the
     * default title, unlike {@code getLocalizedTitle()} which can return null.
     */
    private void resolveCategoryName() {
        if (StringUtils.isBlank(categoryTagId)) { categoryName = StringUtils.EMPTY; return; }
        try {
            TagManager tagManager = currentResource.getResourceResolver().adaptTo(TagManager.class);
            if (tagManager == null) { categoryName = categoryTagId; return; }
            Tag tag = tagManager.resolve(categoryTagId);
            if (tag == null) { categoryName = categoryTagId; return; }
            Locale locale = (request != null) ? request.getLocale() : Locale.ENGLISH;
            categoryName = tag.getTitle(locale);
        } catch (Exception e) {
            LOG.error("Error resolving category tag '{}': {}", categoryTagId, e.getMessage(), e);
            categoryName = categoryTagId;
        }
    }

    /** Called once here — not in the getter — to avoid repeat HTTP calls per render. */
    private void fetchStockCount() {
        if (StringUtils.isBlank(sku) || inventoryService == null) return;
        try {
            stockCount = inventoryService.getStockCount(sku);
        } catch (Exception e) {
            LOG.error("Failed to fetch stock for sku '{}': {}", sku, e.getMessage(), e);
            // stockCount stays -1 — HTL renders a "check availability" fallback
        }
    }

    /**
     * Adapts each child {@code Resource} to {@link ProductVariant}. {@code adaptTo}
     * can return null on failure — the null filter here is mandatory, never skip it.
     */
    private void adaptVariants() {
        if (variantResources == null || variantResources.isEmpty()) {
            variants = Collections.emptyList();
            return;
        }
        variants = variantResources.stream()
                .map(r -> r.adaptTo(ProductVariant.class))
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    private void buildSummary() {
        summary = new ProductSummary(title, sku, formattedPrice, categoryName, stockCount, featured, variants);
    }

    @Override public String getTitle()          { return StringUtils.defaultIfBlank(title, ""); }
    @Override public String getSku()            { return StringUtils.defaultIfBlank(sku, ""); }
    @Override public String getFormattedPrice() { return formattedPrice; }
    @Override public String getCategoryName()   { return categoryName; }
    @Override public int getStockCount()        { return stockCount; }
    @Override public List<ProductVariant> getVariants() { return variants; }
    @Override public boolean isFeatured()       { return featured; }
    @Override public ProductSummary getSummary() { return summary; }
}
