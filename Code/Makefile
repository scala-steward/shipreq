.PHONY: info rm_targets

info:
	@echo "This Makefile isn't how you build ShipReq, but rather a collection of helpful utilities."
	@echo
	@echo "Commands:"
	@echo
	@echo "  * rm_targets -- Removes all local target directories."
	@echo

rm_targets:
	find . -name target -type d | egrep -v './project|frontend' | xargs rm -rfv
