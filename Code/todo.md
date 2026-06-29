* Add `FormulaFieldRef.FormulaField`
  * Prevent cycles in formula fields (add to `DataProp`?)

* Add events for create & update of formula fields
  * Add generic data `CustomFormulaFieldGD`
  * Add codecs
  * Update RandomData and RandomEventStream
  * Update ApplyEvent

* Add commands for create & update of formula fields
  * Add codecs
  * Update MakeEvent

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
