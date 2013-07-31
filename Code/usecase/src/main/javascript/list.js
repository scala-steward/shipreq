function UseCaseSummary(uc) {
    var m = ko.mapping.fromJS(uc)

    m.cssClass = "uc-"+uc.dataEid

    m.editMode = ko.observable(false)

    m.enterEditMode = function(){ m.editMode(true); $("."+m.cssClass+" textarea").select().focus() }

    m.viewUrl = urls.viewUseCase(uc.dataEid)

    return m
}

function UCIViewModel(ucs) {
    this.useCases = ko.observableArray($.map(ucs,UseCaseSummary))
    this.populated = ko.computed(function(){ return this.useCases().length > 0 }, this);
    this.findByDataEid = function(v) { return $.grep(VM.useCases(), function(n){ return n.dataEid() == v })[0] }
}

$(document).ready(function() {
    ko.applyBindings(VM)
});

$(document).on('new-uc', function(event, data) {
    var m = UseCaseSummary(data)
    m.editMode(true)
    VM.useCases.push(m)
    m.enterEditMode()
});

$(document).on('upd-uc', function(event, data) {
    var n = UseCaseSummary(data)
    var m = VM.findByDataEid(n.dataEid())
    VM.useCases.replace(m,n)
});
