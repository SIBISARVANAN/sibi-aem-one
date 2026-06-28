package com.sibi.aem.one.core.models;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sibi.aem.one.core.models.serializer.ProductVariantListSerializer;

import java.util.List;

/**
 * JSON DTO returned by {@link Product#getSummary()} and serialised by the Jackson
 * exporter at {@code <resource>.model.json}.
 *
 * <h3>Custom Serializer — why it's needed</h3>
 * <p>The default Jackson serializer would dump every getter on {@code ProductVariant},
 * including internal fields like {@code availabilityLabel}. The
 * {@code @JsonSerialize(using = ProductVariantListSerializer.class)} annotation
 * delegates serialisation to a custom class that controls exactly which fields
 * appear, and lets us rename {@code variantSku} → {@code sku} in the output.</p>
 *
 * <pre>
 * {
 *   "productSummary": {
 *     "title": "Classic Cotton T-Shirt", "sku": "SHIRT-001",
 *     "formattedPrice": "£29.99", "categoryName": "Apparel",
 *     "stockCount": 45, "featured": true,
 *     "variants": [
 *       { "sku": "SHIRT-001-M-RED", "size": "M", "colour": "Red", "available": true }
 *     ]
 *   }
 * }
 * </pre>
 */
public class ProductSummary {

    private final String title;
    private final String sku;
    private final String formattedPrice;
    private final String categoryName;
    private final int stockCount;
    private final boolean featured;

    @JsonSerialize(using = ProductVariantListSerializer.class)
    public final List<ProductVariant> variants;

    public ProductSummary(String title, String sku, String formattedPrice,
                          String categoryName, int stockCount, boolean featured,
                          List<ProductVariant> variants) {
        this.title = title; this.sku = sku; this.formattedPrice = formattedPrice;
        this.categoryName = categoryName; this.stockCount = stockCount;
        this.featured = featured; this.variants = variants;
    }

    public String getTitle()          { return title; }
    public String getSku()            { return sku; }
    public String getFormattedPrice() { return formattedPrice; }
    public String getCategoryName()   { return categoryName; }
    public int getStockCount()        { return stockCount; }
    public boolean isFeatured()       { return featured; }
    public List<ProductVariant> getVariants() { return variants; }
}
