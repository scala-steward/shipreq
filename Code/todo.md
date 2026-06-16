* Manually test
  * changes to FilterAlgebra
  * decimal places exceeding Int.MaxValue
  * "The maximum can't be larger than the minimum" from UI

* Automated test
  * protocol tests for new events

* Number rendering:
  * Style `_number` in `ProjectWidgets`

* Add to data prop:
  * Ensure custom number field values (in data) are valid (i.e. in config)
  * Same with the req ids

* Number field value validation
  * At least on the server-side, it's currently legal to set fields with out-of-bounds numbers
    * This affects `RandomEventStream` too

* MakeEvent for create{GR,UC}

* Number editor
  * Confirm reusability working
  * Instructions
  * Error: invalid input
    * bad number (eg. "2..")
    * number out of range (or should it be allowed?)

* Number cell
  * Copy
  * Paste
  * Displays defaults
  * Uses set precision for both defaults and specified values
  * Check rendering of out-of-bounds \&/ dead
  * Test field rules apply (i.e. N/A, mandatory, optional, default)

* ReqTable
  * sorting by number
    * respects applicability / defaults by reqType

* Issues
  * Field defaults that are out of range

* Update the changelog
* Run Scalafix
