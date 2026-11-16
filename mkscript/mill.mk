.PHONY: fmt check

fmt:
	mill -i __.reformat

check:
	mill -i __.checkFormat
