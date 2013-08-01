var liftAjax = {
    log: []
    ,lift_ajaxHandler: function(a, b, c, d){
        this.log.push([a,b,c,d])
        //console.log(a)
        return false
    }
    ,last: function () { return this.log[this.log.length - 1] }
    ,lastA: function () { var x = this.last(); return x ? x[0] : x }
    ,clear: function () { this.log = [] }
}

var ids = {
    title: "uc-title"
    ,tf0: "F808585046428RZEI0V"
    ,tf1: "F808585046429ODSPMQ"
    ,tf2: "F808585046430I4NI1A"
    ,s_1_0: "s1049-t"
    ,s_1_0_1: "s1046-t"
    ,s_1_0_2: "s1048-t"
    ,s_1_0_2_a: "s1047-t"
    ,s_1_0_3: "s1028-t"
    ,s_1_1: "s1050-t"
    ,s_1_e_1: "s1043-t"
    ,s_1_e_2: "s1044-t"
    ,tf3: "F8085850464312XOWYC"
    ,tf8: "F8085850464365THSLV"
}

function $id(id) {
    return $("#" + id)
}

function setInitialFocus(elementId) {
    var e = $id(elementId)
    e.focus()
    equal( e.is(':focus'), true, "Initial focus failed." )
    return e
}

function assertRetainsFocus(elementId, fn) {
    var e = setInitialFocus(elementId)
    fn()
    equal( e.is(':focus'), true, "Focus lost." )
}

function assertLosesFocus(elementId, fn) {
    var e = setInitialFocus(elementId)
    fn()
    equal( e.is(':focus'), false, "Focus didn't change." )
}

// ---------------------------------------------------------------------------------------------------------------------

function testBelow(desc, from, to) {
    test(desc, function () { equal(getElementBelow(from).id, to) });
}
function testAbove(desc, from, to) {
    test(desc, function () { equal(getElementAbove(from).id, to) });
}

module("Locates previous input")
testAbove("title -> bottom", ids.title, ids.tf8)
testAbove("text -> title", ids.tf0, ids.title)
testAbove("text -> text", ids.tf2, ids.tf1)
testAbove("NC -> text", ids.s_1_0, ids.tf2)
testAbove("NC -> NC", ids.s_1_0_3, ids.s_1_0_2_a)
testAbove("AC -> NC", ids.s_1_1, ids.s_1_0_3)
testAbove("EC -> AC", ids.s_1_e_1, ids.s_1_1)
testAbove("EC -> EC", ids.s_1_e_2, ids.s_1_e_1)
testAbove("text -> EC", ids.tf3, ids.s_1_e_2)

module("Locates next input")
testBelow("bottom -> title", ids.tf8, ids.title)
testBelow("title -> text", ids.title, ids.tf0)
testBelow("text -> text", ids.tf1, ids.tf2)
testBelow("text -> NC", ids.tf2, ids.s_1_0)
testBelow("NC -> NC", ids.s_1_0_2_a, ids.s_1_0_3)
testBelow("NC -> AC", ids.s_1_0_3, ids.s_1_1)
testBelow("AC -> EC", ids.s_1_1, ids.s_1_e_1)
testBelow("EC -> EC", ids.s_1_e_1, ids.s_1_e_2)
testBelow("EC -> text", ids.s_1_e_2, ids.tf3)

// ---------------------------------------------------------------------------------------------------------------------

module("Keyboard shortcuts", {setup: function(){ liftAjax.clear() } })
function testFocusChange(name, fn, from, to) {
    test(name, function(){
        assertLosesFocus(from, fn)
        equal( $id(to).is(':focus'), true, "Target didn't get focus." )
    })
}
testFocusChange("[Alt + Down] Move focus to next", onAltDown, ids.s_1_0, ids.s_1_0_1)
testFocusChange("[Alt + Up] Move focus to previous",  onAltUp, ids.s_1_0, ids.tf2)

test("[Esc] Drops focus", function(){
    assertLosesFocus(ids.s_1_0, onEscape)
    equal( $(':focus').length, 0, "Nothing should have focus." )
})

test("[Alt + Enter] Creates a new step when a step is selected", function () {
    assertLosesFocus(ids.s_1_0_1, onAltEnter)
    equal( liftAjax.lastA(), "F80858504645403UGSM=true", "New-step RPC should be called." )
})

test("[Alt + Enter] Does nothing when a text field is focused", function () {
    assertRetainsFocus(ids.tf1, onAltEnter)
    equal( liftAjax.lastA(), undefined, "No ajax calls expected." )
})