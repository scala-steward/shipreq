Prototype / UI
==============

## High Freq UIs

* Prototype {UC, SHR, subreqs} on {Incmp, GroupingBrowser}.

## Med Freq UIs

* Mock up allocation graphs.
* The implication browser differs from the grouping one in that it considers
  implication transitivity in its content, but not its editing.
  i.e. remove all from MF-1 implied can still leave some transitively implied reqs remaining.
  Idea: Visually distinguish direct & indirect, have a button to hide indirect.
* GBrowser, so many buttons. Unneeded if just browsing.

Implementation / Other
======================

* Idea: a project-wide long representing revision would be AWESOME.
  Eg. client's cache is at 15348, DB is at 16001, send changes. Could also use associativity and commutativity of (+) to compartmentalise and use revs for each data component (eg. grouping cfg rev, reqs rev) as long as all revs are monotonic.

* Language:
  Groupings     ⇒ Tags
  Incompletions ⇒ Issues
  Sem ID        ⇒ 
  SMR           ⇒ 
  Implication   ⇒ ¿ needs/causes/requires/implies/drives/encompasses ? 

Decisions
=========

* Mistake/accident prevention/recovery strategy.
  Put undo/redo buttons in top-right.
  Undo shows (either directly or on hover) a desc of the change.
* Implications workings in `analysis-subreqs.ods`.

* Deleted rows displayed together/separate, restore/delete by drag/button?
  * Affects: Cfg.Fields, Cfg.ReqTypes, Cfg.Groupings.
  * Make consistent with filter:
    - Button to show/hide deleted.
    - Each row has a control to enable/disable it.
    - Drag to order.

UX Notes
========
* Context over consistency
* ≤ 5 columns in a table
