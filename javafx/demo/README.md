# Simple demo project using JavaFX

How to use vim to develop a Maven project.
Here are brief steps:

* using markdown for memo, create the file *README.md*
* enable syntax highlight inside markdown file
* using the demo JavaFX project for quick test
* add the *exec-maven-plugin* and set the *mainClass*
* How to execute current line under cursor as a bash commands
* pratice the command maven commands

## enable syntax hightlight inside markdown

Add the following into the **~/.vimrc** file:

```vim
" enable syntax highlight inside markdown.
" reference: https://vimtricks.com/p/highlight-syntax-inside-markdown/
" We could come back to change this file again and again...
" need execute :source ~/.vimrc to reload it.
let g:markdown_fenced_languages = ['bash', 'vim', 'nginx', 'sql', 'xml']
```

## enable exec-maven-plugin and set mainClass

Add the following to *pom.xml* file.

```xml
<project>
    <build>
        <plugins>
            <!--
              - add the exec maven plugin to run a Java Main method
              - to run a Java standalone application
              - reference: https://www.baeldung.com/maven-java-main-method
            -->
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.0.0</version>
                <configuration>
                    <mainClass>com.example.demo.HelloApplication</mainClass>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

## vim execute current line as bash command

vim commands:

```vim
" execute current line as bash command(s):
:.w !bash

" try this.
ls -la

" set map
nnoremap \br :.w !bash<CR>

" use this map on last command.

" read the output from a bash:
" the most useful case, read the timestamp
:r!date
```

## Common maven commands:

Following are list of maven commands:

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
