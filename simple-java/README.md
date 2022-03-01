# Playground for simple Java classes

Simple Java classes are Java classes without **package** declaration.

So we could compile and execute them by using **javac** and **java** commands.

## compile and execute inside vim

Here are command we will use:

```vim
:!javac %

:!java -cp %:p:h %:t:r
```

The **echo** command will show the values of those signs:

```vim
:echo @%
echo expand('%:p')
echo expand('%:p:h')
echo expand('%:t:r')
