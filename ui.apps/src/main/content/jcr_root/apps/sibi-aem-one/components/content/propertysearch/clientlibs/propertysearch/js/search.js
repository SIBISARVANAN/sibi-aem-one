(function (document, $) {
    "use strict";

    // Intercept form submit and issue AJAX request to the JSON servlet,
    // updating only the results area without a full page reload.
    $(document).on("submit", ".cmp-propertysearch__form", function (e) {
        e.preventDefault();

        var $form    = $(this);
        var $results = $form.closest(".cmp-propertysearch").find(".cmp-propertysearch__results-area");
        var params   = $form.serialize();

        // Show loading state
        $results.addClass("cmp-propertysearch__results-area--loading");

        // Call the JSON servlet — same URL but with .search.json extension
        var url = window.location.pathname + ".search.json?" + params;

        $.ajax({
            url:      url,
            method:   "GET",
            dataType: "json",
            success: function (data) {
                updateResults(data, $results);
                // Update browser URL so the search is bookmarkable
                if (history.pushState) {
                    history.pushState(null, "", "?" + params);
                }
            },
            error: function () {
                $results.html("<p class='cmp-propertysearch__error'>Search failed. Please try again.</p>");
            },
            complete: function () {
                $results.removeClass("cmp-propertysearch__results-area--loading");
            }
        });
    });

    /**
     * Rebuilds the results area from the JSON response.
     * In a real project, consider using a Handlebars/Mustache template
     * or a small framework — for simplicity, building HTML directly here.
     */
    function updateResults(data, $results) {
        if (!data || data.totalMatches === 0) {
            $results.html("<p class='cmp-propertysearch__no-results'>No properties found.</p>");
            return;
        }

        var html = "<div class='cmp-propertysearch__meta'>" + data.totalMatches + " properties found</div>";
        html += "<div class='cmp-propertysearch__cards'>";

        if (data.hits) {
            data.hits.forEach(function (hit) {
                html += "<article class='cmp-propertysearch__card" +
                        (hit.featured ? " cmp-propertysearch__card--featured" : "") + "'>";
                if (hit.featured) {
                    html += "<div class='cmp-propertysearch__card-badge'>Featured</div>";
                }
                html += "<h3 class='cmp-propertysearch__card-title'>" +
                        "<a href='" + hit.path + ".html'>" + escapeHtml(hit.title || "") + "</a></h3>";
                html += "<p class='cmp-propertysearch__card-meta'>" +
                        escapeHtml(hit.propertyType || "") + " &bull; " + escapeHtml(hit.status || "") + "</p>";
                html += "<p class='cmp-propertysearch__card-price'>" + hit.price + " " +
                        escapeHtml(hit.currency || "") + "</p>";
                html += "<a class='cmp-propertysearch__card-cta' href='" + hit.path + ".html'>View Listing</a>";
                html += "</article>";
            });
        }

        html += "</div>";
        $results.html(html).show();
    }

    // Simple XSS-safe string escaping for dynamically-built HTML
    function escapeHtml(str) {
        return str.replace(/&/g,"&amp;").replace(/</g,"&lt;")
                  .replace(/>/g,"&gt;").replace(/"/g,"&quot;");
    }

})(document, jQuery);
