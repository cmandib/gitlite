# gitlite

A working implementation of Git built from scratch in Java. This project reimplements the core internals of Git, the object store, index, branching, merging, without using any Git libraries. Every command reads and writes files in the exact same binary format that real Git uses, meaning real Git can read objects created by gitlite and vice versa.

---

## What is built

### Object storage

Git stores everything — files, directories, commits, as objects in a content-addressed store under `.git/objects`. Each object is identified by the SHA-1 hash of its contents, compressed with zlib.

gitlite implements three object types:

**Blob** — the raw contents of a file. When you add a file, its bytes are wrapped in a header (`blob <size>\0<content>`), hashed, compressed, and written to disk.

**Tree** — a snapshot of a directory. Each entry in a tree stores the file mode, name, and a raw 20-byte SHA-1 pointer to a blob or another tree. This is how Git represents nested directories.

**Commit** — a pointer to a tree, a pointer to the parent commit, the author and committer identity with timestamp and timezone, and a message. Merge commits have two parent pointers.

---

### Commands

`init` — creates the `.git` directory structure with `objects/`, `refs/heads/`, `refs/tags/`, and a `HEAD` file pointing to the main branch.

`hash-object` — reads a file, hashes it as a blob, and optionally writes it to the object store. Produces the same hash as real Git.

`cat-file -p` — reads any object from the store, decompresses it, strips the header, and prints the contents.

`write-tree` — walks the current working directory recursively, hashes every file as a blob, and builds tree objects bottom-up. Returns the hash of the root tree.

`add` — stages a file by hashing it and recording it in the Git index file at `.git/index`. The index is written in the real Git binary format (DIRC version 2), complete with file metadata, entry padding to 8-byte boundaries, and a SHA-1 checksum of the entire file. Real Git can read this index.

`commit` — reads the staged files from the index, builds a tree object, assembles a commit object with the tree hash, parent commit, author and committer info read from git config, a Unix timestamp with timezone offset, and the commit message. Writes the commit to the object store and advances the branch ref. If a merge is in progress (`.git/MERGE_HEAD` exists), creates a merge commit with two parents.

`log` — reads the current branch ref, loads the commit object, prints the hash, author, human-readable date, and message, then follows the parent pointer and repeats. Traverses both parents of merge commits.

`branch` — lists all branches in `refs/heads/`, marking the current branch with an asterisk. When given a name, creates a new branch pointing to the current commit.

`checkout` — updates `HEAD` to point to another branch, then reads that branch's commit, walks its tree, and restores every file to the working directory.

`status` — compares three things: the last commit, the index, and the working directory. Reports files that are staged for commit, files that have been modified since staging, files that have been deleted, and files that are not tracked.

`diff` — takes two commit hashes, flattens their trees into filename-to-hash maps, and compares them. Uses the longest common subsequence algorithm to produce unified diff output showing added lines, removed lines, and context. Handles added files, deleted files, and modified files.

`merge` — merges another branch into the current branch. First checks if the target branch is already in the current branch's history (already up to date). Then checks if a fast-forward is possible by seeing if the current commit is a direct ancestor of the target. If so, just advances the branch pointer and restores the working directory. Otherwise, finds the common ancestor commit using breadth-first search through the full commit graph including merge commits, then performs a three-way merge: files only changed on one side are taken automatically, files changed differently on both sides get conflict markers written to disk. When conflicts exist, the merge state is saved to `.git/MERGE_HEAD` so the next commit automatically becomes a merge commit with both parents recorded.

---

## How the index works

The index file at `.git/index` is a binary file in Git's DIRC format. It starts with a 4-byte magic header, a version number, and an entry count. Each entry contains file timestamps, size, mode, a 20-byte raw SHA-1, a 2-byte flags field encoding the filename length, the filename, a null terminator, and padding to align to an 8-byte boundary. The file ends with a 20-byte SHA-1 checksum of all preceding bytes. This is identical to what real Git writes, so `git ls-files --stage` can read it directly.

---

## How objects are verified

Every object written by gitlite can be verified with real Git:

```
git cat-file -p <hash>
```

This works because the binary format is identical, same header structure, same zlib compression, same SHA-1 hashing, same raw 20-byte hash storage in tree entries.

---

## Running gitlite

Build the JAR:

```
mvn package
```

This produces `target/gitlite.jar`. With `gitlite.bat` on your PATH pointing to this JAR, you can run commands directly:

```
gitlite init
gitlite add file.txt
gitlite commit -m "first commit"
gitlite log
gitlite branch feature
gitlite checkout feature
gitlite merge main
gitlite diff <hash1> <hash2>
gitlite status
```

All commands operate on the `.git` directory in the current working directory, the same as real Git.

---

## What is not implemented

There is no networking layer, so `push`, `fetch`, `pull`, and `clone` are not implemented. There is no `rebase`. The `log` command only follows the first-parent chain for display, though merge traversal uses both parents internally. There is no `stash`, no `tag`, and no `.gitignore` support.