# maven-collection-baratine

The collection supports generating and building projects that work with Baratine™

## Install 

Clone the project with the following command 

git clone git@github.com:baratine/maven-collection-baratine.git

Change to maven-collection-baratine directory and issue command
./install.sh

This will install the archetype and plugin into your local repository

## Generating Baratine Project

A Baratine project can be generated using the following Maven command:

`mvn archetype:generate -DarchetypeGroupId=io.baratine \
-DarchetypeArtifactId=maven-archetype-baratine \
-DgroupId=org.acme -DartifactId=MyApp \
-DarchetypeVersion=1.0-SNAPSHOT \
-DinteractiveMode=false`


## Building Baratine Project

Baratine requires special packaging which, similarly to a .war, adopts a specific 
directory structure. 

baratine-plugin provides support for building baratine deployment files (.bar) 
via defining support for maven baratine packaging.

For default Maven pom.xml for Baratine see [pom.xml] 
 
[pom.xml]: https://github.com/baratine/maven-collection-baratine/blob/master/baratine-archetype/src/main/resources/archetype-resources/pom.xml Baratine Maven Project

