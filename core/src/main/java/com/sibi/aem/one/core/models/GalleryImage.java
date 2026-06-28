package com.sibi.aem.one.core.models;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

/**
 * Child Sling Model for a single gallery multifield entry.
 * Adapted from {@code Resource} only — multifield children have no servlet request.
 */
@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class GalleryImage {

    @ValueMapValue
    private String image;   // path written by the fileupload widget
    @ValueMapValue
    private String caption;

    public String getImagePath() {
        return image;
    }

    public String getCaption() {
        return caption;
    }
}
