package com.sibi.aem.one.core.models;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class Store {

    @ValueMapValue private String storeName;
    @ValueMapValue private String storeLink;

    public String getStoreName() { return storeName; }
    public String getStoreLink() {
        if (storeLink != null && storeLink.startsWith("/content/")) {
            return storeLink + ".html";
        }
        return storeLink;
    }
}