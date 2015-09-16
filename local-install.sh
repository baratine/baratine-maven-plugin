#!/bin/bash

cd baratine-maven-archetype
mvn -e clean install site

cd ../baratine-maven-plugin
mvn -e clean install site
