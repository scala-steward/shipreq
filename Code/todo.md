* Manually test
  * changes to FilterAlgebra
  * decimal places exceeding Int.MaxValue
  * "The maximum can't be larger than the minimum" from UI

* Automated test
  * protocol tests for new events

* Number rendering:
  * Style `_number` in `ProjectWidgets`

* Number field value validation
  * At least on the server-side, it's currently legal to set fields with out-of-bounds numbers
    * This affects `RandomEventStream` too

* Update the changelog
* Run Scalafix
