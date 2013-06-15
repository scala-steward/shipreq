function modelise(uc) {
    var m = ko.mapping.fromJS(uc)

    m.cssClass = "u"+uc.vid

    m.editMode = ko.observable(false)

    m.save = submitJsonForm(apiUrls.updateUseCaseHeader(uc.vid), 'PUT', function(result) {
        var n = modelise(result)
        VM.useCases.replace(m,n)
        $(document).enhanceDom()
    })

    return m
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
    m.editMode(true)
    VM.useCases.push(m)
    $(document).enhanceDom();
    $("."+m.cssClass+" textarea").select().focus()
});
