package com.sibi.aem.one.core.models;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

/**
 * Sling Model for a single product variant — one size/colour combination.
 *
 * <h3>How child resource injection works</h3>
 * <p>The product dialog has a multifield stored under the component's JCR node:
 * <pre>
 *   jcr:content/
 *     variants/          ← container node
 *       item0/  size=M  colour=Red  variantSku=ABC-M-RED  stock=12
 *       item1/  size=L  colour=Blue variantSku=ABC-L-BLU  stock=0
 * </pre>
 * In {@code ProductImpl}, the field:
 * <pre>{@literal @}ChildResource(name = "variants")
 * private List{@literal <}Resource{@literal >} variantResources;</pre>
 * injects the list of child {@code Resource} objects. Each is then adapted to
 * {@code ProductVariant.class} (this class) in {@code @PostConstruct} of {@code ProductImpl}.</p>
 *
 * <h3>Adaptable: Resource.class only</h3>
 * <p>Child resources don't have their own servlet request, so this model is
 * adapted from {@code Resource} only — this is the key reason child models
 * always use {@code adaptables = Resource.class}.</p>
 */
@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ProductVariant {

    private static final Logger LOG = LoggerFactory.getLogger(ProductVariant.class);

    @ValueMapValue private String size;
    @ValueMapValue private String colour;
    @ValueMapValue private String variantSku;
    @ValueMapValue private int stock;

    /** Derived in @PostConstruct so the logic lives in Java, not HTL. */
    private boolean inStock;
    private String availabilityLabel;

    @PostConstruct
    protected void init() {
        this.inStock = stock > 0;
        if (stock > 10)      this.availabilityLabel = "In Stock";
        else if (stock > 0)  this.availabilityLabel = "Only " + stock + " left";
        else                 this.availabilityLabel = "Out of Stock";
        LOG.debug("ProductVariant init — sku={}, stock={}, label={}", variantSku, stock, availabilityLabel);
    }

    public String getSize()              { return size; }
    public String getColour()            { return colour; }
    public String getVariantSku()        { return variantSku; }
    public int getStock()                { return stock; }
    public boolean isInStock()           { return inStock; }
    public String getAvailabilityLabel() { return availabilityLabel; }
}
