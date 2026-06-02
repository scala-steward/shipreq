# Project deletion

### Server-side

* [ ] Add deletion reason & liveness to project
* [ ] Add new events to delete/restore project
* [ ] Add new commands to delete/restore project
* [ ] Upgrade protocols
* [ ] Update random data (don't generate normal events when dead in RandomEventStream)
* [ ] Prevent updating of dead projects (ProjectSpaLogic & TestGlobal)
* [ ] DB: Add live field to project table and update it on delete/restore
* [ ] Update project metadata to include a live field

### Client-side

* [ ] Factor project liveness into editability
* [ ] Make a status page
* [ ] Add deletion UI
* [ ] Add restoration UI
* [ ] Add live/dead filter to client-home

### Other

* [ ] Update changelog
