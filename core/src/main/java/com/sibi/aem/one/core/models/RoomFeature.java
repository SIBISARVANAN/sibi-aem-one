package com.sibi.aem.one.core.models;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

/**
 * Leaf-level child model for the NESTED multifield: each {@code Room} has a
 * {@code features} multifield, and each feature entry is one of these —
 * a single {@code value} property. This is the second level of
 * {@code @ChildResource} adaptation (Room -> RoomFeature), demonstrating
 * that nested multifield support works by simply chaining the same pattern
 * one level deeper, with each level still {@code adaptables = Resource.class}.
 */
@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class RoomFeature {

    @ValueMapValue
    private String value;

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    } // convenient for HTL ${feature}
}
