(function (document) {
    "use strict";

    // Fires a CLICK event into the data layer when a visitor clicks "RSVP for
    // Open House." Reads the property ID from the data-property-id attribute
    // that PropertyDataLayerModel rendered server-side — JS never invents or
    // duplicates that value, it only decides WHEN to send it.
    document.addEventListener("click", function (e) {
        var button = e.target.closest(".property-listing__rsvp-button");
        if (!button) {
            return;
        }

        window.adobeDataLayer = window.adobeDataLayer || [];
        window.adobeDataLayer.push({
            event: "openHouseRsvpClick",
            property: {
                propertyId: button.getAttribute("data-property-id")
            }
        });
    });

})(document);
