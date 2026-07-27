# Staging copy — this project's real home is its own repo

This folder is a **temporary transport copy** of the MD5Deep GUI project. It
lives on the `claude/md5deep-windows-gui-mvo3x9` branch only because this
Claude session's GitHub integration cannot create repositories (403), and the
session container is ephemeral — pushing here was the only way to preserve the
work. Per the repo rule, new software projects get their own repo; **do not
merge this folder into MedCounter's `main`.**

To move it to its own repo:

1. On GitHub, create an empty repo `robbstacy/md5deep-gui` (no README/license —
   the project already has both), and grant the Claude GitHub app access to it.
2. Ask Claude to push this folder there (it will recreate the standalone
   history and delete this staging copy), **or** do it by hand:

   ```
   git clone https://github.com/robbstacy/MedCounter -b claude/md5deep-windows-gui-mvo3x9 tmp
   mkdir md5deep-gui && cp -r tmp/md5deep-gui/* md5deep-gui/ && cd md5deep-gui
   rm _STAGING_NOTE.md
   git init -b main && git add -A && git commit -m "MD5Deep GUI 1.0"
   git remote add origin https://github.com/robbstacy/md5deep-gui
   git push -u origin main
   ```

3. Delete this `md5deep-gui/` folder from the MedCounter branch (or just delete
   the whole branch — it was created only for this task).
