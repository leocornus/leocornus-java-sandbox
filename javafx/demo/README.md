# Simple demo project using JavaFX

Common maven commands:

```bash
# check maven version
mvn --version

# we need Java 11 or higher to using JavaFX
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home; mvn --version

# compile source code.
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home; cd ~/rd/java-sandbox/javafx/demo; mvn compile

# compile source code and run the app
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home; cd ~/rd/java-sandbox/javafx/demo; mvn compile exec:java
```
