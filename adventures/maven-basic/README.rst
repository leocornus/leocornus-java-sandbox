Basic Tasks for Using Apache Maven
==================================

We will explore the following tasks:

* create a Java project
* compile
* execute test
* package to jar file

Pre-request
-----------

We will need the following:

* JDK, this is a java project.
* Apache Maven, it will be just a simple download and de-compress.
  Java binary will be checked JAVA_HOME and the first choice.
  If there is no JAVA_HOME the output of command **which java** will 
  be used.

That's it!

Simple tasks
------------

Create Project:

mvn archetype:generate -DgroupId=com.leocorn.sandbox.maven -DartifactId=basic-tasks -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false

Here are the simpl tasks:

* cd basic-tasks
* mvn compile
* mvn test
* mvn package
* mvn -DskipTests package
