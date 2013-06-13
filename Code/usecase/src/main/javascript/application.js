// require "vendor/jquery.js"
// require "vendor/jquery-ui.js"
// require "vendor/jquery-autosize.js"
// require "vendor/jquery-timeago.js"

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

$(document).ready(function () {
    $(document).enhanceDom();
});
