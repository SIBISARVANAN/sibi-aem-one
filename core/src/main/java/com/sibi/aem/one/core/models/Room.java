package com.sibi.aem.one.core.models;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Child model for one {@code rooms} multifield entry. Demonstrates the
 * NESTED MULTIFIELD pattern: this model itself is adapted from a parent
 * multifield's child resource (Room), and ALSO contains its own
 * {@code @ChildResource} for the inner {@code features} multifield,
 * adapting each entry to {@link RoomFeature}.
 *
 * <p>This is the exact pattern requested: "nested multifield" = a multifield
 * whose own child resources contain another multifield.</p>
 */
@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class Room {

    private static final Logger LOG = LoggerFactory.getLogger(Room.class);

    @ValueMapValue private String roomName;
    @ValueMapValue private String roomType;
    @ValueMapValue private Integer areaSqft;

    /** Injects the inner "features" multifield's children as Resources. */
    @ChildResource(name = "features")
    private List<Resource> featureResources;

    private List<String> features;

    @PostConstruct
    protected void init() {
        if (featureResources == null || featureResources.isEmpty()) {
            features = Collections.emptyList();
            return;
        }
        features = featureResources.stream()
                .map(r -> r.adaptTo(RoomFeature.class))
                .filter(Objects::nonNull)
                .map(RoomFeature::getValue)
                .collect(Collectors.toList());
        LOG.debug("Room '{}' adapted {} nested features", roomName, features.size());
    }

    public String getRoomName()      { return roomName; }
    public String getRoomType()      { return roomType; }
    public Integer getAreaSqft()     { return areaSqft; }
    public List<String> getFeatures() { return features; }
}
