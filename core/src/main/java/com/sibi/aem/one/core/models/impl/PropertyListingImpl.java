package com.sibi.aem.one.core.models.impl;

import com.day.cq.tagging.TagManager;
import com.sibi.aem.one.core.models.GalleryImage;
import com.sibi.aem.one.core.models.PropertyListing;
import com.sibi.aem.one.core.models.Room;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Named;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Implementation of {@link PropertyListing}.
 *
 * <p>Reads every field authored via the Touch UI dialog and derives display-ready
 * values: formatted price, resolved amenity tag names, and open-house date-range
 * validity. Demonstrates {@code @ChildResource} for both the flat gallery
 * multifield and the nested rooms→features multifield (via {@link Room}).</p>
 */
@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class PropertyListingImpl implements PropertyListing {

    private static final Logger LOG = LoggerFactory.getLogger(PropertyListingImpl.class);
    private static final DateTimeFormatter OPEN_HOUSE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @ValueMapValue
    @Named("jcr:title")
    private String title;
    @ValueMapValue
    private String subtitle;
    @ValueMapValue
    private String shortDescription;
    @ValueMapValue
    private String fullDescription;
    @ValueMapValue
    private String status;
    @ValueMapValue
    private String soldDate;
    @ValueMapValue
    private boolean featured;
    @ValueMapValue
    private String badgeColor;
    @ValueMapValue
    private String themeColor;
    @ValueMapValue
    private String propertyType;
    @ValueMapValue
    private String locationPagePath;
    @ValueMapValue
    private String agentPagePath;
    @ValueMapValue
    private Double price;
    @ValueMapValue
    private String currency;
    @ValueMapValue
    private String contactPreference;
    @ValueMapValue
    private String availableFrom;
    @ValueMapValue
    private String openHouseStart;
    @ValueMapValue
    private String openHouseEnd;
    @ValueMapValue
    private boolean searchVisible;

    /**
     * Multi-value tag IDs from the tagpicker (cq/gui/.../form/tagfield).
     */
    @ValueMapValue
    private String[] amenityTags;

    /**
     * Flat multifield — each gallery entry adapted to {@link GalleryImage}.
     */
    @ChildResource(name = "gallery")
    private List<Resource> galleryResources;

    /**
     * Outer level of the NESTED multifield — each entry adapted to {@link Room}.
     */
    @ChildResource(name = "rooms")
    private List<Resource> roomResources;

    @Self
    private Resource currentResource;

    private List<GalleryImage> galleryImages;
    private List<Room> rooms;
    private List<String> amenityNames;
    private String formattedPrice;
    private boolean openHouseRangeValid;

    @PostConstruct
    protected void init() {
        adaptGallery();
        adaptRooms();
        resolveAmenityNames();
        buildFormattedPrice();
        validateOpenHouseRange();
    }

    private void adaptGallery() {
        if (galleryResources == null) {
            galleryImages = Collections.emptyList();
            return;
        }
        galleryImages = galleryResources.stream()
                .map(r -> r.adaptTo(GalleryImage.class))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Each Resource here is adapted to Room, which internally adapts its own nested features.
     */
    private void adaptRooms() {
        if (roomResources == null) {
            rooms = Collections.emptyList();
            return;
        }
        rooms = roomResources.stream()
                .map(r -> r.adaptTo(Room.class))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Resolves each stored tag ID to its localised title via {@link TagManager}.
     * {@code tag.getTitle(locale)} never returns null, unlike {@code getLocalizedTitle()}.
     */
    private void resolveAmenityNames() {
        if (amenityTags == null || amenityTags.length == 0) {
            amenityNames = Collections.emptyList();
            return;
        }
        TagManager tagManager = currentResource.getResourceResolver().adaptTo(TagManager.class);
        if (tagManager == null) {
            amenityNames = List.of(amenityTags);
            return;
        }
        amenityNames = List.of(amenityTags).stream()
                .map(tagManager::resolve)
                .filter(Objects::nonNull)
                .map(t -> t.getTitle(Locale.ENGLISH))
                .collect(Collectors.toList());
    }

    private void buildFormattedPrice() {
        if (price == null) {
            formattedPrice = "Price on request";
            return;
        }
        try {
            NumberFormat fmt = NumberFormat.getCurrencyInstance(Locale.US);
            if (StringUtils.isNotBlank(currency)) {
                fmt.setCurrency(java.util.Currency.getInstance(currency));
            }
            formattedPrice = fmt.format(price);
        } catch (Exception e) {
            LOG.warn("Could not format price with currency '{}': {}", currency, e.getMessage());
            formattedPrice = String.format("%.2f %s", price, StringUtils.defaultIfBlank(currency, ""));
        }
    }

    /**
     * Validates that {@code openHouseEnd} is chronologically after {@code openHouseStart}.
     * This is the validation AEM's two-field "range" pattern requires, since there is
     * no single native range datepicker widget.
     */
    private void validateOpenHouseRange() {
        if (StringUtils.isBlank(openHouseStart) || StringUtils.isBlank(openHouseEnd)) {
            openHouseRangeValid = false;
            return;
        }
        try {
            LocalDateTime start = LocalDateTime.parse(openHouseStart, OPEN_HOUSE_FORMAT);
            LocalDateTime end = LocalDateTime.parse(openHouseEnd, OPEN_HOUSE_FORMAT);
            openHouseRangeValid = end.isAfter(start);
            if (!openHouseRangeValid) {
                LOG.warn("Open house end '{}' is not after start '{}' for resource {}",
                        openHouseEnd, openHouseStart, currentResource.getPath());
            }
        } catch (DateTimeParseException e) {
            LOG.warn("Could not parse open house dates: {}", e.getMessage());
            openHouseRangeValid = false;
        }
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getSubtitle() {
        return subtitle;
    }

    @Override
    public String getShortDescription() {
        return shortDescription;
    }

    @Override
    public String getFullDescription() {
        return fullDescription;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public String getSoldDate() {
        return soldDate;
    }

    @Override
    public boolean isFeatured() {
        return featured;
    }

    @Override
    public String getBadgeColor() {
        return badgeColor;
    }

    @Override
    public String getThemeColor() {
        return themeColor;
    }

    @Override
    public String getPropertyType() {
        return propertyType;
    }

    @Override
    public List<GalleryImage> getGalleryImages() {
        return galleryImages;
    }

    @Override
    public List<Room> getRooms() {
        return rooms;
    }

    @Override
    public String getLocationPagePath() {
        return locationPagePath;
    }

    @Override
    public String getAgentPagePath() {
        return agentPagePath;
    }

    @Override
    public double getPrice() {
        return price != null ? price : 0d;
    }

    @Override
    public String getFormattedPrice() {
        return formattedPrice;
    }

    @Override
    public String getCurrency() {
        return currency;
    }

    @Override
    public List<String> getAmenityNames() {
        return amenityNames;
    }

    @Override
    public String getContactPreference() {
        return contactPreference;
    }

    @Override
    public String getAvailableFrom() {
        return availableFrom;
    }

    @Override
    public String getOpenHouseStart() {
        return openHouseStart;
    }

    @Override
    public String getOpenHouseEnd() {
        return openHouseEnd;
    }

    @Override
    public boolean isOpenHouseRangeValid() {
        return openHouseRangeValid;
    }

    @Override
    public boolean isSearchVisible() {
        return searchVisible;
    }
}
