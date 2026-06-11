* Manually test
  * changes to FilterAlgebra
  * decimal places exceeding Int.MaxValue
  * "The maximum can't be larger than the minimum" from UI

* Automated test
  * new MakeEvent functionality
  * new ApplyEvent functionality
  * protocol tests for new events

* Number rendering:
  * Style `_number` in `ProjectWidgets`

* On the server-side, it's possible to generate a faulty number field by updating only Min or only Max

* Number field value validation
  * At least on the server-side, it's currently legal to set fields with out-of-bounds numbers
    * This affects `RandomEventStream` too
  * Updating a number value with the same `Option[Double]` doesn't count as `Unchanged`
    * This affects `RandomEventStream` too
