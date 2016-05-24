#!/usr/bin/env bash

ag \
TODO \
src/ README.md pom.xml "$@"
