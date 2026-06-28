package com.sibi.aem.one.core.models;

import java.util.List;

/**
 * Sling Model interface for the Product Detail component.
 *
 * <p>Scenario: an e-commerce product page component. The dialog stores basic
 * fields (title, SKU, price) and a multifield of product variants (size/colour
 * combinations), each stored as a child JCR node under the component node.</p>
 */
public interface Product {

    String getTitle();
    String getSku();

    /** Derived, display-ready price string e.g. "£49.99", computed in @PostConstruct. */
    String getFormattedPrice();

    /** Category name resolved from a stored tag ID via TagManager. */
    String getCategoryName();

    /** Live stock count from the inventory service; -1 if the service call failed. */
    int getStockCount();

    /** Adapted from child JCR nodes via @ChildResource — the key pattern demonstrated. */
    List<ProductVariant> getVariants();

    boolean isFeatured();

    /** JSON-serialisable summary DTO, exposed via the Jackson exporter at .model.json. */
    ProductSummary getSummary();
}
