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
