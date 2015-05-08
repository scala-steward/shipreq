Next Steps
==========

Refs in ReqTitles

Rules – ReqTypes / field

ReqCode Refs – parse
ReqCode Refs – display
ReqCode Refs – auto-complete

Show deleted – toggle
Show deleted – filter
Show deleted – effect on displayed refs
Show deleted – effect on auto complete
Show deleted – effect on validation

Table – no rows
Table – status bar

Issues – Highlight empty cells
Issues – Highlight imp required
Issues – Tag enum violations

#### Then…

Filters
New row
Cfg hard/soft deletion
Real project storage
Project data mod protocol
Detail view (modal?)

================================================================================

Phase 2
=======

### ReqTable

* Modification protocol
* Filters
* Highlight empty, mandatory cells
* Create new req + new req template
* Req Codes
  * Display
  * Edit
  * Groups
* [Change req-type] button + modal + logic
* [Change common sem-ID prefix] button + modal + logic
* Deleted stuff
  * Hide
  * Handle in display widgets
  * Handle in editors
* Copy & paste
* Bulk restore? Or delete? (Re-read reqs)
* Row detail view

### Other

* Cfg screens & usage/deleted
  * Count usage
  * Show usage
  * Prevent deletion
* Loose issue
* Issues screen
  * Screen (composite of views, filter, buttons, summary)
  * Distribution view & data representation
  * Detail view section: Blank
  * Detail view section: Custom
  * Detail view section: Loose
  * Detail view section: Cfg issues
* Deleting/Restoring Reqs
  * Data representation + derivation
  * Tree n-level checkboxes
  * Component


================================================================================

* From ideas.md:
    TODOs due to MF intersection problems.
    where/how to store intersection problems?
    Neither loose nor in-req incmps seem to be enough for intersection probs.

* Maybe its time to revisit the UX book(s).

* Field based on implication just like they have with groupings. Except it
  should be read-only and show resolve transitively to a given req-type.
  So I can create a field showing the driving MF for everything.

* Implications from UC steps ← how?
* Implications from UC fields ← allow?
* Implications from LL fields ← no!

