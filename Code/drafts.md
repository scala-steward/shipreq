Goals
=====

* Drafts are saved (per user) locally and remotely

* A live draft is automatically updated when the underlying field is changed
  Sources:
    * it's externally and directly edited
    * a change somewhere else in the project affects the field

* Restoration. Either:
  1) The most recently aborted drafts can be restored
  2) Drafts can be "hidden"/deferred and then later resumed

* [LATER] Users can see each others' live drafts (read-only)
  For now, isolating streams by user id means there will be less surface area for "conflicts".
  We can always stitch multiple drafts streams together at runtime.
  Either that's the approach we take in future, or in future we merge steams holistically which will
  be easier to do later once all the other pieces are in place.

* [MAYBE] Users can share (all? some?) live drafts with each other (meaning both edit the same draft & commit saves both)

* [MAYBE] Pruning.
  Prune drafts from DB when...
    1) known to be useless, and/or
    2) by age
  Or maybe we just keep draft streams forever, it's only 50% larger than content apparently.


Meta
====
* Start simple, add more functionality later/incrementally - just ensure that it's possible to add later
* Work out the model & strategy by building incrementally by feature
* Consider and test all resulting scenarios by users PoV (after functionality proven possible)


Plan
====
* Create DraftFeature (in isolation)
* Replace EditorFeature state with drafts
* Handle abort (whilst still maintaining the draft stream/state)
* Handle commit (whilst still maintaining the draft stream/state)
* Handle field changes by external events
* Sync drafts with remote
* Sync drafts with WW
