# simple readme

## set git author

```bash
# set git author:
cd ~/rd/java-sandbox; git config user.name 'Sean Chen'
# set the author email
cd ~/rd/java-sandbox; git config user.email 'sean.chen@leocorn.com'
cd ~/rd/java-sandbox; git config --list

cd ~/rd/java-sandbox; git log
```

##  chage user info in a git commit

```bash
cd ~/rd/java-sandbox; git commit --amend --reset-author
```

## set up maven

We need set up maven to load correct Java.

```bash
# check PATH.
echo $PATH

# set up maven executable.

# check maven version
mvn --version

# Sat 28 May 2022 16:16:45 EDT
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home; mvn --version

# comple the javafx demo's source code.
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home; cd ~/rd/java-sandbox/javafx/demo; mvn compile

```
