// JQuery is required separately.
// - 1) It's 93KB, separation allows for me parallelism.
// - 2) Will probably switch to CDN later.

// require "vendor/jquery-ui.js"
// require "vendor/jquery-autosize.js"
// require "vendor/jquery-timeago.js"
// require "vendor/jquery-serializeObject.js"
// require "vendor/jquery-livequery.js"
// require "vendor/jquery-rangyinputs.js"
// require "vendor/mousetrap.js"
// require "vendor/mousetrap-global-bind.js"

/**
 * Predicate that returns true if an element is visible to the user.
 */
function isVisible(e) {
    return $(e).filter(':visible').css('visibility') != 'hidden'
}

(function ($) {
    // JQuery's filter() provides the index as the fn arg. This uses the element.
    $.fn.filterE = function (fn) {
        return this.filter(function(i){return fn(this)})
    };
}(jQuery));

// ---------------------------------------------------------------------------------------------------------------------

var urls = new function() {
    this.viewUseCase = function(id){ return "/usecase/"+id }
};

// Add a global event handler to make Enter submit the current form, for any elements with class 'enterSubmitsForm'.
$(document).keypress(function (e) {
    if (e.which === 13 && e.target.classList.contains('enterSubmitsForm')) {
        e.preventDefault();
        e.stopPropagation();
        $(e.target).parents("form").find("input[type=submit]:visible:first").focus().click();
    }
})

DomEnhancements = [
    {css: "abbr.timeago", apply: function(x){ x.timeago() }},
    {css: "textarea",     apply: function(x){ x.autosize() }}
];

function registerDomEnhancementsWithLiveQuery() {
    for (var i = 0; i < DomEnhancements.length; i++) {
        var e = DomEnhancements[i]
        // console.debug("Registering LQ: "+ e.css)
        $(e.css).livequery(function (ee) {
            return function () {
                // console.debug("LQ calling: "+ ee.css)
                ee.apply($(this))
            }
        }(e))
    }
}
$(document).ready(registerDomEnhancementsWithLiveQuery)

function enhanceDom() { $(document).enhanceDom() }
(function ($) {
    // Provide JQuery fn to apply DomEnhancements
    $.fn.enhanceDom = function () {
        for (var i=0; i < DomEnhancements.length; i++) {
            var e = DomEnhancements[i]
            e.apply(this.find(e.css))
        }
        return this;
    };
}(jQuery));