package com.sibi.aem.one.core.models;

import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Sling Model wrapping a "Neighborhood Guide" Content Fragment, referenced
 * from a Property Listing via the {@code neighborhoodFragmentPath} pathfield.
 *
 * <h2>Real-World Scenario</h2>
 * <p>Each property listing links to a separately-authored, reusable
 * Content Fragment describing the surrounding neighborhood — walkability
 * score, school rating, and nearby amenities. The SAME fragment is reused
 * across every property listing in that neighborhood, so it's modeled as a
 * Content Fragment (structured, headless, reusable) rather than being
 * duplicated as plain text inside every property page.</p>
 *
 * <h2>Content Fragment Model fields this expects</h2>
 * <p>Defined under {@code /conf/sibi-aem-one/settings/dam/cfm/models/neighborhood-guide}:
 * {@code title} (text), {@code walkabilityScore} (number, 0-100),
 * {@code schoolRating} (number, 0-10), {@code summary} (multiline text),
 * {@code nearbyAmenities} (multi-value text).</p>
 *
 * <h2>Adaptation pattern</h2>
 * <p>This model is adapted from the {@code Resource} AT THE FRAGMENT'S OWN PATH
 * (e.g. {@code /content/dam/sibi-aem-one/neighborhood-guides/downtown}) — NOT
 * from the property listing's resource. The caller (PropertyListingImpl) resolves
 * {@code neighborhoodFragmentPath} to a Resource first, then adapts that resource
 * to this model. This is the standard two-step pattern for any "reference field"
 * pointing at a separate piece of content.</p>
 */
@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class NeighborhoodGuide {

    private static final Logger LOG = LoggerFactory.getLogger(NeighborhoodGuide.class);
    /**
     * Sling Models don't have a built-in {@code @ContentFragment} injector —
     * the standard approach is to adapt the resource explicitly in
     * {@code @PostConstruct} rather than relying on field injection.
     */
    private final Resource resource;
    private String title;
    private String summary;
    private int walkabilityScore;
    private double schoolRating;
    private List<String> nearbyAmenities;
    private boolean valid;

    public NeighborhoodGuide(Resource resource) {
        this.resource = resource;
    }

    @PostConstruct
    protected void init() {
        ContentFragment fragment = resource.adaptTo(ContentFragment.class);
        if (fragment == null) {
            // Common cause: the path doesn't point to a DAM asset backed by a
            // Content Fragment Model, or the fragment is a draft not yet published.
            LOG.warn("Resource at '{}' did not adapt to ContentFragment", resource.getPath());
            valid = false;
            return;
        }

        title = readText(fragment, "title");
        summary = readText(fragment, "summary");
        walkabilityScore = readInt(fragment, "walkabilityScore");
        schoolRating = readDouble(fragment, "schoolRating");
        nearbyAmenities = readStringArray(fragment, "nearbyAmenities");
        valid = true;
    }

    /**
     * Reads a single text element. {@code fragment.getElement(name)} returns null
     * if the element doesn't exist in this fragment's model — always null-check
     * before calling {@code getContent()}, since a model field rename/removal
     * after fragments are already authored is a common source of NPEs here.
     */
    private String readText(ContentFragment fragment, String elementName) {
        ContentElement element = fragment.getElement(elementName);
        return element != null ? element.getContent() : "";
    }

    private int readInt(ContentFragment fragment, String elementName) {
        ContentElement element = fragment.getElement(elementName);
        if (element == null) return 0;
        try {
            // getValue(Class) handles the structured-data-type elements
            // (number/boolean/date) — NOT getContent(), which is text-only.
            Integer value = element.getValue().getValue(Integer.class);
            return value != null ? value : 0;
        } catch (Exception e) {
            LOG.warn("Could not read int element '{}': {}", elementName, e.getMessage());
            return 0;
        }
    }

    private double readDouble(ContentFragment fragment, String elementName) {
        ContentElement element = fragment.getElement(elementName);
        if (element == null) return 0d;
        try {
            Double value = element.getValue().getValue(Double.class);
            return value != null ? value : 0d;
        } catch (Exception e) {
            LOG.warn("Could not read double element '{}': {}", elementName, e.getMessage());
            return 0d;
        }
    }

    private List<String> readStringArray(ContentFragment fragment, String elementName) {
        ContentElement element = fragment.getElement(elementName);
        if (element == null) return Collections.emptyList();
        try {
            String[] values = element.getValue().getValue(String[].class);
            return values != null ? Arrays.asList(values) : Collections.emptyList();
        } catch (Exception e) {
            LOG.warn("Could not read multi-value element '{}': {}", elementName, e.getMessage());
            return Collections.emptyList();
        }
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public int getWalkabilityScore() {
        return walkabilityScore;
    }

    public double getSchoolRating() {
        return schoolRating;
    }

    public List<String> getNearbyAmenities() {
        return nearbyAmenities;
    }

    public boolean isValid() {
        return valid;
    }
}
