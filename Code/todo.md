# Formulas referencing formulas

* Add `FormulaFieldRef.FormulaField`
  * Prevent cycles in formula fields (add to `DataProp`?)
  * Prevent referencing self in formula fields (add to `DataProp`?)

* Update to include formula fields:
  * formula auto-complete
  * formula help
  * RandomData

* Issue detection
  * Live formulas that reference dead formula fields (and test transitivity)

* scalafix
* changelog
