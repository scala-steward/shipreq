* Issue detection
  * Formula field reqType rules are all dead or N/A *(rule already in-place, confirm works)*

* changelog

# Formulas referencing formulas

* Add `FormulaFieldRef.FormulaField`
  * Prevent cycles in formula fields (add to `DataProp`?)
  * Prevent referencing self in formula fields (add to `DataProp`?)

* Update formula auto-complete to include formula fields

* Issue detection
  * Live formulas that reference dead formula fields (and test transitivity)

* scalafix
* changelog
