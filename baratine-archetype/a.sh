#!/usr/bin/env bash

mvn archetype:generate -o -DarchetypeGroupId=io.baratine \
-DarchetypeArtifactId=maven-archetype-baratine \
-DgroupId=my.group -DartifactId=MyArtifact \
-DarchetypeVersion=1.0-SNAPSHOT \
-DinteractiveMode=false
