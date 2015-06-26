#!/bin/bash

export MAVEN_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005
mvn -e -X -P run-its clean integration-test
