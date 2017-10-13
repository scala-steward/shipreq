# Dashboard
* Add an issues page. Put the Σ of issues on it. Make it red if !0.
* Change layout, maybe make 4x2

# Req Table/Detail
* [T-] Add each type of issue to filter DSL
* [TD] Field and text highlighting (red text, red bkgd, strikethrough)

# Issues screen

* Needs filter for content
  * (?) should it only apply to visible fields, or just be the same as ReqTable?
    Hint: Filter passing on non-visible content will be confusing and imply a defect

* Needs filter for issue types
  * Note: the DSL for this should be applied to ROWS, not REQS.
    Eg. if UC-2 has blank fields and conflicting tags, using `hasIssue:blank` shouldn't select UC-2 and then display all its issues; it should hide the row with conflicting tags.

* (?) Allow column selection? (eg. show/hide a Scope column)
  Hint: Scope is the only default column that they might switch off
  Hint: Users might want to add columns like MF, Priority, etc.
  DECISION: YES! Allow column selection

* Show text+icon+hover desc of Σ and breakdown (like in ReqTable)

* Try a different impl approach here. Create a representation of the entire table in memory, then draw it second.
  Should make it easier to consolidate adjacent rows.
  How much information can be applied in abstract? Row hovering, N/A fields, etc.
  Maybe ReqTable is already doing this at a reasonable level...

# Logic
* Consider UC steps empty if `.text.isEmpty && .flow.nonEmpty`?
  Only consider the text field. flow without text = error

================================================================================

# For the backlog

* How to see/undo closed loose issues?
  Leave it for now

* [T-] Add an issue count column.
    Postponing for now because it introduces too much overlap with the Issues table.
    It might even be removed from scope entirely later.

* [T-] (?) Add a footer row and so user can see total/sum number of issues in scope
           What else would go in the footer in other columns?
           #low-pri. Skip for now & add to back

* Ideas to address the accidently-resolve-find-undo scenario:
 * Add a list of created/resolved ORDER BY creation/resolution date DESC
 * Add last-updated column to ReqTable
 * Add event log screen. Actually the [n changes] text in the dashboard could be a good link

# Rejected

* [-D] (?) Add issues field with Σ and <ul>
           Nice to have? Try the kano method

AWESOME!
+--------------------|---------------------
|                    |
|                    |
|                    |
|                    |                    @
|                    @
+-@------------------|--------------------- Fully impl
|                    |
|                    |
|                    |
|                    |
|                    |
+--------------------|---------------------
FU!
