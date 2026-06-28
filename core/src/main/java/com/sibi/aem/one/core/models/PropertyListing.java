package com.sibi.aem.one.core.models;

import java.util.List;

/**
 * Sling Model interface for the Property Listing component.
 *
 * <p>Backs a Touch UI dialog demonstrating every requested Coral UI 3 field
 * type and layout (tabs, fixedcolumns, accordion, multifield, nested
 * multifield, dropdown/checkbox showhide, RTE, pathfield, fileupload,
 * colorfield/swatch, datepicker pair, tagfield, popover). See the dialog's
 * {@code _cq_dialog/.content.xml} for the authored field definitions this
 * model reads from.</p>
 */
public interface PropertyListing {

    String getTitle();

    String getSubtitle();

    String getShortDescription();

    /**
     * Raw RTE HTML — rendered with {@code @ context='html'} in HTL.
     */
    String getFullDescription();

    String getStatus();

    String getSoldDate();

    boolean isFeatured();

    String getBadgeColor();

    String getThemeColor();

    String getPropertyType();

    /**
     * Adapted from the {@code gallery} multifield child nodes.
     */
    List<GalleryImage> getGalleryImages();

    /**
     * Adapted from the {@code rooms} multifield; each room has its own nested {@code features} list.
     */
    List<Room> getRooms();

    String getLocationPagePath();

    String getAgentPagePath();

    double getPrice();

    /**
     * Price combined with the currency code, e.g. "$450,000.00".
     */
    String getFormattedPrice();

    String getCurrency();

    /**
     * Resolved tag titles from the {@code amenityTags} tagpicker, via TagManager.
     */
    List<String> getAmenityNames();

    String getContactPreference();

    String getAvailableFrom();

    String getOpenHouseStart();

    String getOpenHouseEnd();

    /**
     * @return true if openHouseEnd is chronologically after openHouseStart.
     * Demonstrates the date-range validation logic that AEM's two-field
     * "range" pattern requires, since there is no native range widget.
     */
    boolean isOpenHouseRangeValid();

    boolean isSearchVisible();
}
