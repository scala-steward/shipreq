# Dashboard
Add an issues page. Put the Σ of issues on it. Make it red if !0.

# Req Table
Add an issue count column.

# Req Detail
Add issues field with Σ and <ul>.

# Both issue views
* [FR-113] Show Req id
* [FR-61 ] Show Req title
* [FR-62 ] Show Req field name (eg. "title", "detail", "Step 2.3.1")
* [FR-63 ] Show issue sub-text
* [FR-64 ] Abbreviate long text (maybe not… at least for not now)


# Dist view
Could make it a tree like:
```
All
  Config
    Fields
    Tags
  Content
    Code Groups
    FRs
      FR-1
      FR-7
    UCs
  Loose
```

# By-Type view


# TBD
* Add a list of created/resolved ORDER BY creation/resolution date DESC
* Confirm the purpose/needs for each view and whether there need to be two views.
* Prototype each type of issue in each view
* Consider UC steps empty if `.text.isEmpty && .flow.nonEmpty`?

# Real, underlying requirements for issue views
*
