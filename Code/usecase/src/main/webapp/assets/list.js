function modelise(uc) {
    uc.edit = false
    uc.save = function(form) {
        this.edit(false)
    }
    uc.cssClass = "u"+uc.vid
    return ko.mapping.fromJS(uc)
}

function UseCaseIndexModel(ucs) {
    this.useCases = ko.observableArray($.map(ucs,modelise))
    this.populated = ko.computed(function(){ return this.useCases().length > 0 }, this);
}

$(document).ready(function() {
    ko.applyBindings(VM)
    $(document).enhanceDom()
});

$(document).on('new-uc', function(event, data) {
    var m = modelise(data)
    m.edit(true)
    VM.useCases.push(m)
    $(document).enhanceDom();
    $("."+m.cssClass()+" textarea").select().focus()
});
