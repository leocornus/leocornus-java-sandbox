# Playground for simple Java classes

Simple Java classes are Java classes without **package** declaration.

So we could compile and execute them by using **javac** and **java** commands.

## compile and execute Java inside vim

Here are command we will use:

```vim
:!javac %

:!java -cp %:p:h %:t:r
```

The sign **%:p:h** is called filename modifiers.
The **echo** command will show the values of those signs:

```vim
" show the relative path to current file.
:echo @%
" show the full path to current file.
:echo expand('%:p')
# show the full apth to the current folder.
:echo expand('%:p:h')
# the name of the current file without extension
:echo expand('%:t:r')
```

```bash
# ls current folder.
# it only works for the exec format:
# :exec '!'.getline('.')
# it does NOT work for the .w format:
# :.w !bash
ls -la %:p:h
```

Here is [a list filename modifiers](http://vimdoc.sourceforge.net/htmldoc/cmdline.html#filename-modifiers).
The only help will have some details too:
```vim
:help filename-modifiers
```

## create shortcut by using map in vim

Here is a map to compile and execute Java class at once.
The following will create shortcut **\cr** to compile and execute the java file
in current buffer.
```vim
# create shortcut \cr
nnoremap <leader>cr :!javac -verbose %<CR>:!java -cp %:p:h %:t:r<CR>
# check existing map
:map
```

How to find out what is **leader**?

```vim
:help leader
```

## save the the shortcut for future usage

We could store the shortcut in **.vimrc** or vim session file.

After you set up shortcut in the vim session you are working on,
the **mksession** will store the map in vim session file.
Next time if you load the vim session by using **source** command,
you have have the shortcut ready to use.

The file **.vimrc** is another option to store your chortcut.
Just add the following line in your **.vimrc** file,
you will have the shortcut loaded whenever you are using vim.
```vim
# in file ~/.vimrc
nnoremap <leader>cr :!javac -verbose %<CR>:!java -cp %:p:h %:t:r<CR>
```
