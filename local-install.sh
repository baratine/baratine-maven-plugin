#!/bin/bash

cd baratine-maven-archetype
mvn -e clean install

cd ../baratine-maven-plugin
mvn -e clean install
