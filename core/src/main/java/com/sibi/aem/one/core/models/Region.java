package com.sibi.aem.one.core.models;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import java.util.Collections;
import java.util.List;

@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class Region {

    @ValueMapValue
    private String regionName;

    @ChildResource(name = "stores")
    private List<Store> stores;

    public String getRegionName() { return regionName; }

    public List<Store> getStores() {
        return stores != null ? stores : Collections.emptyList();
    }
}