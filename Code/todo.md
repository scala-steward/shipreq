* Add `FormulaFieldRef.FormulaField`
  * Prevent cycles in formula fields (add to `DataProp`?)
  * Prevent referencing self in formula fields (add to `DataProp`?)

* To fix:
  * Add auto-complete to formula editor

* Manually test UI
  * Issues screen
    * Formula cells
      * Display okay?
      * KBNav

* Filters
  * Allow filtering based on formula field values (just like with number fields)
  * Validate `field:Score=default` works as expected
  * Validate `field:Score<op>123` works as expected for all comparison ops

* Issue detection
  * Live formulas that reference out-of-bounds numbers
  * Live formulas that reference dead fields
    * Numeric fields
    * Formula fields (and test transitivity)
  * Live formula cells that contain errors
  * Formula field reqType rules are all dead or N/A (rule already in-place, confirm works)

* scalafix
* changelog
