package com.sibi.aem.one.core.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Produces the Adobe Experience Platform / Adobe Launch data-layer JSON payload
 * for a Property Listing page.
 *
 * <h2>Real-World Scenario</h2>
 * <p>On every property listing page view, the business wants Adobe Analytics
 * (via Adobe Launch) to capture: property ID, listing price, property type,
 * and status. Separately, when a visitor clicks "RSVP for Open House," a
 * distinct CLICK event must fire with the same property ID — for funnel
 * analysis between "viewed listing" and "RSVP'd."</p>
 *
 * <h2>Design: page-load data is server-rendered, interaction events are client-fired</h2>
 * <p>This model only handles the PAGE-LOAD half. It wraps the existing
 * {@link PropertyListing} model (composition, not duplication of its fields)
 * and serialises a data-layer-shaped JSON object, pushed into
 * {@code window.adobeDataLayer} via a small inline script in HTL — BEFORE the
 * Adobe Launch embed script loads, so Launch's page-load rules see real data
 * immediately rather than an empty/stale data layer.</p>
 *
 * <p>The separate RSVP CLICK event is fired entirely in JavaScript (see
 * {@code open-house-rsvp.js}), reading property ID from a {@code data-*}
 * attribute this same model renders into the markup — keeping this Java model
 * the single source of truth for what the property ID actually IS, while JS
 * only decides WHEN to push the click event.</p>
 */
@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class PropertyDataLayerModel {

    private static final Logger LOG = LoggerFactory.getLogger(PropertyDataLayerModel.class);

    @Self private SlingHttpServletRequest request;
    @Self private Resource resource;

    private PropertyListing property;
    private String propertyId;
    private String dataLayerJson;

    @PostConstruct
    protected void init() {
        // Compose with the existing PropertyListing model rather than re-reading
        // the same JCR properties a second time — single source of truth.
        property = request.adaptTo(PropertyListing.class);
        if (property == null) {
            LOG.debug("Resource {} did not adapt to PropertyListing — skipping data layer", resource.getPath());
            dataLayerJson = "{}";
            return;
        }

        // Stable identifier for analytics — using the JCR node name here;
        // in production, prefer an explicit authored SKU/ID field if one exists.
        propertyId = resource.getName();

        dataLayerJson = buildJson();
    }

    /**
     * Builds the data-layer payload using a plain {@code Map} + Jackson —
     * the same DTO-then-serialize approach used for {@code ProductSummary}
     * earlier in this project, just inlined into the page instead of exposed
     * as a public REST endpoint.
     */
    private String buildJson() {
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("pageType", "property-listing");

        Map<String, Object> propertyData = new LinkedHashMap<>();
        propertyData.put("propertyId", propertyId);
        propertyData.put("price", property.getPrice());
        propertyData.put("currency", property.getCurrency());
        propertyData.put("propertyType", property.getPropertyType());
        propertyData.put("status", property.getStatus());
        propertyData.put("featured", property.isFeatured());

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("page", page);
        root.put("property", propertyData);

        try {
            return new ObjectMapper().writeValueAsString(root);
        } catch (Exception e) {
            LOG.error("Failed to serialise data layer JSON for {}: {}", resource.getPath(), e.getMessage(), e);
            return "{}";
        }
    }

    /** Raw JSON, pushed into {@code window.adobeDataLayer} via an inline HTL script. */
    public String getDataLayerJson() { return dataLayerJson; }

    /** Exposed separately so HTL can also render it as a {@code data-property-id} attribute for the RSVP button. */
    public String getPropertyId() { return propertyId; }
}
