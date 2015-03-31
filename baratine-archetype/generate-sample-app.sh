#!/usr/bin/env bash

mvn archetype:generate -DarchetypeGroupId=io.baratine \
-DarchetypeArtifactId=maven-archetype-baratine \
-DgroupId=org.acme -DartifactId=MyApp \
-DarchetypeVersion=1.0-SNAPSHOT \
-DinteractiveMode=false
