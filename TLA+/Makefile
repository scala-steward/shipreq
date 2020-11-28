TLC := tlc-colour -difftrace

info:
	@echo "Commands:"
	@echo
	@echo "  * drafts[-watch]"
	@echo "  * encryption[-watch]"
	@echo "  * project[-watch]"
	@echo "  * websocket_client[-watch]"
	@echo "  * websockets[-watch]"
	@echo


.PHONY: drafts drafts-watch
drafts:
	$(TLC) drafts
drafts-watch:
	ls -1 drafts* Makefile | entr -cr make drafts


.PHONY: encryption encryption-watch
encryption:
	$(TLC) encryption
encryption-watch:
	ls -1 encryption* Makefile | entr -cr make encryption


.PHONY: project project-watch
project:
	./project
project-watch:
	ls -1 project* Makefile | entr -cr make project


.PHONY: websocket_client websocket_client-watch
websocket_client:
	$(TLC) websocket_client
websocket_client-watch:
	ls -1 websocket_client* Makefile | entr -cr make websocket_client


.PHONY: websockets websockets-watch
websockets:
	$(TLC) websockets
websockets-watch:
	ls -1 websockets* Makefile | entr -cr make websockets
