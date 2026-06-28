package com.sibi.aem.one.core.models;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class CtaButton {

    @ValueMapValue private String text;
    @ValueMapValue private String link;

    public String getText() { return text; }
    public String getLink() {
        if (link != null && link.startsWith("/content/")) {
            return link + ".html";
        }
        return link;
    }
}