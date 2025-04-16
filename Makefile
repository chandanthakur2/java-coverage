GRADLE := ./gradlew

.PHONY: build test

build:
	$(GRADLE) build

test:
	$(GRADLE) test

.PHONY: run
run:
	$(GRADLE) bootRun

.PHONY: clean
clean:
	$(GRADLE) clean

.PHONY: help
help:
	@echo "Available targets:"
	@echo "  build - Build the project"
	@echo "  test - Run tests"
	@echo "  run - Run the application"
	@echo "  clean - Clean the project"
	@echo "  help - Show this help message"

.PHONY: default
default: build

.PHONY: all
all: build test run
