* Add `FormulaFieldRef.FormulaField`
  * Prevent cycles in formula fields (add to `DataProp`?)
  * Prevent referencing self in formula fields (add to `DataProp`?)

* WCP compilation
  * Add UI for formula fields
    * Add formula editor
    * Add editor for `FieldReqTypeRules` for read-only fields
    * Figure out a shorter summary for req-type rules on the lhs of the screen
  * Add a Sorter for `ErrorMsg \/ FormulaValue`
  * Render `ErrorMsg \/ FormulaValue`
    * What do to about alignment in `ReqTable`?
      * Numbers should be right-aligned but it's defined at the cell level
      * Headers should be right-aligned if contents (are all / contain) numbers
      * ✅ Maybe just right-align no matter what

* Manually test UI
  * Display of formula cells in both ReqTable and ReqDetail
    * When field is Live
    * When field is Dead
  * Styling of formula cells when eval is Dead and/or Invalid
  * KBNav in both ReqTable and ReqDetail
  * For reqtypes that are N/A to a formula field:
    * Ensure formula field isn't rendered in ReqDetail
    * Ensure formula cell is rendered as N/A in ReqTable
  * Issues screen
    * Formula cells
      * Display okay?
      * KBNav
  * Sorting by formulas in ReqTable

* Filters
  * Allow filtering based on formula field values (just like with number fields)
  * Validate `field:Score=default` works as expected
  * Validate `field:Score<op>123` works as expected for all comparison ops

* Issue detection
  * Live formulas that reference dead fields
    * Numeric fields
    * Formula fields (and test transitivity)
    * `FormulaEvalCache.Eval.validity` should be `Invalid` - test renders as an issue in UI
  * Live formula cells that contain errors
  * Formula field reqType rules are all dead or N/A (rule already in-place, confirm works)

* scalafix
* changelog
