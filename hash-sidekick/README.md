# Hash Sidekick

A Windows 11 sidekick for **[Jesse Kornblum's md5deep](https://github.com/jessek/hashdeep)** —
a native GUI for the workflow his command-line tools made standard: recursive
file hashing, known-hash matching, and md5deep/hashdeep-compatible output.
100% local, no network access, no dependencies.

## Credit where it's due

**md5deep and hashdeep were created by Jesse Kornblum.** His tools (`md5deep`,
`sha1deep`, `sha256deep`, `hashdeep`, …) defined this workflow and remain the
reference implementation. Hash Sidekick is deliberately named something else:
it's a companion that puts a window on that workflow, shares no code with the
original, and makes no claim on the md5deep name. It is not affiliated with or
endorsed by the original project.

- Original project: <https://github.com/jessek/hashdeep>
- Historical home: <http://md5deep.sourceforge.net>

md5deep is public domain software; Hash Sidekick is public domain too (see
[LICENSE](LICENSE)).

## Features

- **Recursive hashing** — add files or whole folders; "Include subfolders"
  mirrors `md5deep -r`. Drag & drop works too.
- **Multiple algorithms in one pass** — MD5, SHA-1, SHA-256, SHA-512
  (the md5deep family: `md5deep`, `sha1deep`, `sha256deep`). Each file is read
  once no matter how many algorithms are selected.
- **Known-hash matching** — load a hash list (md5deep output, hashdeep output,
  CSV, or bare hashes) and every file is marked **MATCH** / **no match**, like
  `md5deep -m`. Useful for verifying downloads or auditing a folder.
- **Compatible output** — save results as a plain `hash  filename` list
  (md5deep style), hashdeep audit format (`%%%% HASHDEEP-1.0`), or CSV.
- Progress bar with cancel, per-file error reporting (hover a row marked
  *error* for details), copy rows to clipboard.
- Runs fine on locked-down machines: no installer, no admin rights
  (`asInvoker`), no network, per-monitor DPI aware.

## Building on Windows 11 Pro

### Option A — zero install (recommended)

Windows 11 ships with the .NET Framework 4.8 runtime, which includes a C#
compiler. **Double-click `build.bat`** — it produces `HashSidekick.exe` in the
same folder. Nothing to download or install.

### Option B — .NET SDK

If you have the [.NET 8 SDK](https://dotnet.microsoft.com/download) (or newer):

```
dotnet build -c Release
```

The executable lands in `bin/Release/net8.0-windows/`.

Either build produces the same app; Option A targets .NET Framework 4.8 and
Option B targets .NET 8.

## Using it

1. Run `HashSidekick.exe`.
2. Add files/folders (buttons, or drag & drop onto the window).
3. Tick the algorithms you want and press **Compute Hashes**.
4. Optional: **Matching → Load Known Hashes…** to verify files against a hash
   list — rows turn **MATCH** (green) or **no match** (red).
5. **File → Save Results…** to export in md5deep, hashdeep, or CSV format.

## Notes

- Files that can't be read (locked, access denied) are reported per-file and
  don't abort the run — same spirit as `md5deep -r` soldiering on.
- MD5 and SHA-1 are provided for compatibility with existing hash sets; for
  new integrity baselines prefer SHA-256 or SHA-512.
