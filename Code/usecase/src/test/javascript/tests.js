var liftAjax = {
    log: []
    ,lift_ajaxHandler: function(a, b, c, d){
        this.log.push([a,b,c,d])
        //console.log(a)
        return false
    }
    ,last: function() { return this.log[this.log.length - 1] }
    ,lastA: function() { var x = this.last(); return x ? x[0] : x }
    ,clear: function() { this.log = [] }
}

var ids = {
    title: {txt: "uc-title"}
    ,tf0: {txt: "F808585046428RZEI0V"}
    ,tf1: {txt: "F808585046429ODSPMQ"}
    ,tf2: {txt: "F808585046430I4NI1A"}
    ,s_1_0: {txt: "s1049-t", lbl: "s1049-l", cont: "s1049", flbl: "1.0"}
    ,s_1_0_1: {txt: "s1046-t", lbl: "s1046-l", cont: "s1046", flbl: "1.0.1"}
    ,s_1_0_2: {txt: "s1048-t", lbl: "s1048-l", cont: "s1048", flbl: "1.0.2"}
    ,s_1_0_2_a: {txt: "s1047-t", lbl: "s1047-l", cont: "s1047", flbl: "1.0.2.a"}
    ,s_1_0_3: {txt: "s1028-t", lbl: "s1028-l", cont: "s1028", flbl: "1.0.3"}
    ,s_1_1: {txt: "s1050-t", lbl: "s1050-l", cont: "s1050", flbl: "1.1"}
    ,s_1_e_1: {txt: "s1043-t", lbl: "s1043-l", cont: "s1043", flbl: "1.E.1"}
    ,s_1_e_2: {txt: "s1044-t", lbl: "s1044-l", cont: "s1044", flbl: "1.E.2"}
    ,tf3: {txt: "F8085850464312XOWYC"}
    ,tf8: {txt: "F8085850464365THSLV"}
}

function $id(id) {
    if (id.substr) return $("#" + id)
    throw "ID string expected. Got: "+id
}

function setFocus(elementId) {
    var e = $id(elementId)
    e.focus()
    equal( e.is(':focus'), true, "Element should get focus." )
    return e
}

function assertRetainsFocus(elementId, fn) {
    var e = setFocus(elementId)
    fn()
    equal( e.is(':focus'), true, "Element should retain focus." )
}

function assertLosesFocus(elementId, fn) {
    var e = setFocus(elementId)
    fn()
    equal( e.is(':focus'), false, "Element should lose focus." )
}

function testEach(data, fn) {
    if (data.length)
        return function(){ for (i = 0; i < data.length; i++) { fn(data[i]) }}
    else
        return function(){ for (e in data) { fn(data[e]) }}
}

// ---------------------------------------------------------------------------------------------------------------------

module("Inspection")

var elementsBelow = [
    [ids.tf8,       ids.title],
    [ids.title,     ids.tf0],
    [ids.tf1,       ids.tf2],
    [ids.tf2,       ids.s_1_0],
    [ids.s_1_0_2_a, ids.s_1_0_3],
    [ids.s_1_0_3,   ids.s_1_1],
    [ids.s_1_1,     ids.s_1_e_1],
    [ids.s_1_e_1,   ids.s_1_e_2],
    [ids.s_1_e_2,   ids.tf3]
]

test("getElementAbove", testEach(elementsBelow, function(e) {
    var from = e[1].txt
    var to = e[0].txt
    equal(getElementAbove(from).id, to, to + " <-- " + from)
}))
test("getElementBelow", testEach(elementsBelow, function(e) {
    var from = e[0].txt
    var to = e[1].txt
    equal(getElementBelow(from).id, to, from + " --> " + to)
}))

test("getFullLabel", testEach(ids, function(e) {
    if (e.flbl) {
        equal(getFullLabel($id(e.lbl)), e.flbl)
        equal(getFullLabel($id(e.lbl).parent()), e.flbl)
    }
}))

// ---------------------------------------------------------------------------------------------------------------------

module("Keyboard shortcuts", {setup: function(){ liftAjax.clear() } })
function testFocusChange(name, fn, from, to) {
    test(name, function(){
        assertLosesFocus(from, fn)
        equal( $id(to).is(':focus'), true, "Target didn't get focus." )
    })
}
testFocusChange("[Alt + Down] Move focus to next", onAltDown, ids.s_1_0.txt, ids.s_1_0_1.txt)
testFocusChange("[Alt + Up] Move focus to previous",  onAltUp, ids.s_1_0.txt, ids.tf2.txt)

test("[Esc] Drops focus", function(){
    assertLosesFocus(ids.s_1_0.txt, onEscape)
    equal( $(':focus').length, 0, "Nothing should have focus." )
})

test("[Alt + Enter] Creates a new step when a step is selected", function() {
    assertLosesFocus(ids.s_1_0_1.txt, onAltEnter)
    equal( liftAjax.lastA(), "F80858504645403UGSM=true", "New-step RPC should be called." )
})

test("[Alt + Enter] Does nothing when a text field is focused", function() {
    assertRetainsFocus(ids.tf1.txt, onAltEnter)
    equal( liftAjax.lastA(), undefined, "No ajax calls expected." )
})

// ---------------------------------------------------------------------------------------------------------------------

module('Typing helpers', {setup: enhanceDom})

function assertTypingMode(on) {
    var v = $('#uce').hasClass('typing')
    if (on) equal(v, true, "Typing-mode should be on.")
    else equal(v, false, "Typing-mode should be off.")
}

test("Typing-mode should be on when an input field has focus", function() {
    assertTypingMode(false)
    setFocus(ids.tf2.txt);             assertTypingMode(true)
    var e = setFocus(ids.s_1_0_2.txt); assertTypingMode(true)
    e.blur();                          assertTypingMode(false)
})
