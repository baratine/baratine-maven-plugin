#!/usr/bin/env bash

if [ -z $1 ] || [ -z $2 ] || [ -z $3 ]; then
  echo "usage";

  echo "$0 <groupId> <package> <artifactId>";

  exit;
fi

groupId=$1;
package=$2;
artifactId=$3;

mvn archetype:generate -DarchetypeGroupId=io.baratine \
-DarchetypeArtifactId=maven-archetype-baratine \
-DgroupId=$groupId -Dpackage=$package -DartifactId=$artifactId \
-DarchetypeVersion=1.0-SNAPSHOT \
-DinteractiveMode=false
