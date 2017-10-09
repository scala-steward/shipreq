# Dashboard
Add an issues page. Put the Σ of issues on it. Make it red if !0.

# Req Table
Add an issue count column.

# Req Detail
Add issues field with Σ and <ul>.

# Req Table & Req Detail

* Mark mandatory empty fields red.
  Hover with issue reason.

* Mark choice tag fields with more than one value red.
  If no custom field, mark the Tags column red.
  Hover with issue reason.

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

# Other
* [FR-307] Deleted tags in text-based columns of live reqs should be rendered red or similar to indicate an issue.
* Issue tag in text: live = that issue, dead = dead tag in use.

# TBD
* Add a list of created/resolved ORDER BY creation/resolution date DESC
* Allow issue tag deletion when in use? If yes, explain impact
* Confirm the purpose/needs for each view and whether there need to be two views.
* Prototype each type of issue in each view

# Real, underlying requirements for issue views
*
