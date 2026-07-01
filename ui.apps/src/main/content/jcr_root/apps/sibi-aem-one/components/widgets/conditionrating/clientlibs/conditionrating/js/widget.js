(function (document, $) {
    "use strict";

    // foundation-contentloaded fires whenever Coral loads/reloads dialog content —
    // covers both initial dialog open and any AJAX-refreshed sections.
    $(document).on("foundation-contentloaded", function (e) {
        $(".sibi-ConditionRating", e.target).each(function () {
            var $widget = $(this);
            var $input  = $widget.find(".sibi-ConditionRating-input");
            var $stars  = $widget.find(".sibi-ConditionRating-stars");
            var max     = parseInt($stars.data("max"), 10);

            if ($stars.children().length === 0) {
                for (var i = 1; i <= max; i++) {
                    $("<span class='sibi-ConditionRating-star' data-value='" + i + "'>&#9733;</span>")
                        .appendTo($stars);
                }
            }

            function render(value) {
                $stars.find(".sibi-ConditionRating-star").each(function () {
                    $(this).toggleClass("is-filled", parseInt($(this).data("value"), 10) <= value);
                });
            }

            render(parseInt($input.val(), 10) || 0);

            $stars.off("click").on("click", ".sibi-ConditionRating-star", function () {
                var value = $(this).data("value");
                // .trigger("change") is mandatory — Coral's dialog dirty-tracking and
                // submit logic both listen for native "change" events on form inputs.
                // Without this, the star clicks update visually but never actually save.
                $input.val(value).trigger("change");
                render(value);
            });
        });
    });

})(document, Granite.$);
