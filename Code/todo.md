# Project deletion

### Server-side

* [x] Add deletion reason & liveness to project
* [x] Add new events to delete/restore project
* [x] Add new commands to delete/restore project
* [x] Upgrade protocols (and add protocol tests)
* [x] Update random data (don't generate normal events when dead in RandomEventStream)
* [x] Prevent updating of dead projects (ProjectSpaLogic & TestGlobal)
* [x] DB: Add live field to project table and update it on delete/restore
* [x] Update project metadata to include a live field

### Client-side

* [x] Factor project liveness into editability
* [x] Make a status page
* [x] Add unit tests for status page
* [x] Add deletion UI (admin-only)
* [x] Add restoration UI (admin-only)
* [x] Add live/dead filter to client-home

### Other

* [ ] Update changelog
