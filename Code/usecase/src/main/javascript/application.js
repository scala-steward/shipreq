// JQuery is required separately.
// - 1) It's 93KB, separation allows for me parallelism.
// - 2) Will probably switch to CDN later.

// require "vendor/jquery-ui.js"
// require "vendor/jquery-autosize.js"
// require "vendor/jquery-timeago.js"
// require "vendor/jquery-serializeObject.js"
// require "vendor/jquery-livequery.js"

var apiUrls = new function() {
    this.updateUseCaseHeader = function(id){ return {url: "/api/usecase/"+id, type: 'PUT' }}
}

var urls = new function() {
    this.viewUseCase = function(id){ return "/usecase/"+id }
}

function fullStop(sentence) {
    if (sentence.match('\\.$') == null) return sentence + ".";
    return sentence
}

function ajaxErrorHandler(xhr, textStatus, errorThrown) {
    var genericMsgNeeded = true
    var genericMsg = "Something went wrong. Please try again."
        + "\nIf the problem persists, reload the page and give it another try."
        + "\n\n"

    var msg = "Error " + xhr.status

    var err = null
    if (xhr.status != errorThrown) err = errorThrown
    else if (textStatus != "error") err = textStatus
    if (err != null) msg += ": " + fullStop(err)

    if ([409,412,422,423,428].indexOf(xhr.status) >= 0 && xhr.responseText != "") {
        var limit = 160
        var r = fullStop(xhr.responseText)
        if (r.length > limit) r = r.substring(0, limit) + "..."
        msg += (err == null ? ": " : "\nFeedback: ") + r
        genericMsgNeeded = false
    }

    if (genericMsgNeeded) msg = genericMsg + msg

    alert(msg)
}

function submitJsonForm(apiUrl, successCallback) {
    return function(form) {
        $.ajax({
            url: apiUrl.url,
            type: apiUrl.type,
            contentType: 'application/json',
            dataType: 'json',
            data: JSON.stringify($(form).serializeObject()),
            error: ajaxErrorHandler,
            success: function(data, textStatus, xhr) {
                var result = JSON.parse(xhr.responseText)
                successCallback(result)
            }
        })
    }
}

function enterSubmitsFormHandler(e) {
    if (e.which === 13) {
        e.preventDefault();
        e.stopPropagation();
        $(e.target).parents("form").find("input[type=submit]").focus().click();
    }
}

DomEnhancements = [
    {css: "abbr.timeago",      fn: function(x){ x.timeago() }},
    {css: "textarea",          fn: function(x){ x.autosize() }},
    {css: ".enterSubmitsForm", fn: function(x){ x.keypress(enterSubmitsFormHandler) }}
];

// Apply DomEnhancements via LiveQuery
for (var i = 0; i < DomEnhancements.length; i++) {
    var e = DomEnhancements[i]
    // console.debug("Registering LQ: "+ e.css)
    $(e.css).livequery(function (ee) {
        return function () {
            // console.debug("LQ calling: "+ ee.css)
            ee.fn($(this))
        }
    }(e))
}

// Provide JQuery fns to apply DomEnhancements
function enhanceDom() { $(document).enhanceDom() }
(function ($) {
    $.fn.enhanceDom = function () {
        for (var i=0; i < DomEnhancements.length; i++) {
            var e = DomEnhancements[i]
            e.fn(this.find(e.css))
        }
        return this;
    };
}(jQuery));