* Focus on getting a business partner.
  * Write down list of desired traits & attributes. Have someone query their network.

* Phase 2:
  * Ignore subsequent phases completely.
  * Do minimum set of features that users must have.
  * Use it to get funding.
  * If a lack of a feature would annoy users but not enough for them to stop
    using the system, drop it & add it once you have the users. Users are
    generally willing to tolerate such things.

* Auto-save stuff:
  * Apple does it like that: no apply/save/cancel. (No undo seemingly.)
  * Worry about history & concurrency later.
  * SI: Store operations as changes, load by applying ops, save a total state
    (snapshot) every now and then for performance.
  * Undo = revert.
