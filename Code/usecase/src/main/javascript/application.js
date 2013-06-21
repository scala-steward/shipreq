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

(function ($) {
    $.fn.enhanceDom = function () {
        this.find("abbr.timeago").timeago();
        this.find('textarea').autosize();
        this.find('.enterSubmitsForm').keypress(enterSubmitsFormHandler);
        return this;
    };
}(jQuery));
function enhanceDom() { $(document).enhanceDom() }

$(document).ready(function () {
    $("abbr.timeago").livequery(function(){ $(this).timeago() });
    $('textarea').livequery(function(){ $(this).autosize() });
    $('.enterSubmitsForm').livequery(function(){ $(this).keypress(enterSubmitsFormHandler) });
});
