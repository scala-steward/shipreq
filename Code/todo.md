# Project deletion

### Server-side

* [x] Add deletion reason & liveness to project
* [x] Add new events to delete/restore project
* [ ] Add new commands to delete/restore project
* [x] Upgrade protocols (and add protocol tests)
* [x] Update random data (don't generate normal events when dead in RandomEventStream)
* [ ] Prevent updating of dead projects (ProjectSpaLogic & TestGlobal)
* [ ] DB: Add live field to project table and update it on delete/restore
* [ ] Update project metadata to include a live field

### Client-side

* [ ] Factor project liveness into editability
* [ ] Make a status page
* [ ] Add deletion UI (admin-only)
* [ ] Add restoration UI (admin-only)
* [ ] Add live/dead filter to client-home

### Other

* [ ] Update changelog
