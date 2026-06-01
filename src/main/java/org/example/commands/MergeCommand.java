package org.example.commands;

import org.example.storage.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class MergeCommand {

    public static void execute(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: merge <branch>");
        }

        String targetBranch = args[1];
        Path targetRef = Path.of(".git", "refs", "heads", targetBranch);

        if (!Files.exists(targetRef)) {
            throw new IllegalStateException("Branch not found: " + targetBranch);
        }

        String currentBranch = resolveBranch();
        String currentHash = resolveRef(currentBranch);
        String targetHash = Files.readString(targetRef).trim();

        if (currentHash.equals(targetHash)) {
            System.out.println("Already up to date.");
            return;
        }

        // target is already an ancestor of current — nothing to do
        if (isAncestor(targetHash, currentHash)) {
            System.out.println("Already up to date.");
            return;
        }

        // Check if fast-forward is possible
        if (isAncestor(currentHash, targetHash)) {
            fastForward(currentBranch, targetHash);
            System.out.println("Fast-forward merge to " + targetHash.substring(0, 7));
            return;
        }

        // Find common ancestor for three-way merge
        String ancestor = findCommonAncestor(currentHash, targetHash);

        if (ancestor == null) {
            throw new IllegalStateException("No common ancestor found");
        }

        System.out.println("Merge base: " + ancestor.substring(0, 7));


        // Flatten all three trees
        Map<String, String> ancestorFiles = getFiles(ancestor);
        Map<String, String> currentFiles  = getFiles(currentHash);
        Map<String, String> targetFiles   = getFiles(targetHash);

        // Perform three-way merge
        boolean hasConflicts = MergeEngine.merge(
                ancestorFiles, currentFiles, targetFiles);

        if (hasConflicts) {
            // Save merge state so commit can create a proper merge commit
            Files.writeString(Path.of(".git", "MERGE_HEAD"), targetHash + "\n");
            Files.writeString(Path.of(".git", "MERGE_MSG"),
                    "Merge branch '" + targetBranch + "'\n");
            System.out.println("Automatic merge failed; fix conflicts and commit.");
            return;
        }
        // No conflicts — write tree from current working directory and commit
        String treeHash = TreeWriter.write(Path.of("."));
        String message = "Merge branch '" + targetBranch + "'";
        String commitHash = CommitWriter.writeMergeCommit(
                treeHash, currentHash, targetHash, message);

        updateRef(currentBranch, commitHash);

        System.out.println("Merge made by three-way merge.");
        System.out.println("[commit " + commitHash + "] " + message);
    }

    private static boolean isAncestor(String ancestor, String descendant) throws Exception {
        java.util.Queue<String> queue = new java.util.LinkedList<>();
        java.util.Set<String> visited = new java.util.HashSet<>();
        queue.add(descendant);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current == null || visited.contains(current)) continue;
            visited.add(current);
            if (current.equals(ancestor)) return true;

            CommitParser.CommitObject commit = CommitParser.parse(
                    ObjectReader.read(current));
            if (commit.parent() != null)  queue.add(commit.parent());
            if (commit.parent2() != null) queue.add(commit.parent2());
        }

        return false;
    }

    private static String findCommonAncestor(String a, String b) throws Exception {
        // Collect all ancestors of a via BFS
        java.util.Set<String> ancestorsA = new java.util.HashSet<>();
        java.util.Queue<String> queue = new java.util.LinkedList<>();
        queue.add(a);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current == null || ancestorsA.contains(current)) continue;
            ancestorsA.add(current);

            CommitParser.CommitObject commit = CommitParser.parse(
                    ObjectReader.read(current));
            if (commit.parent() != null)  queue.add(commit.parent());
            if (commit.parent2() != null) queue.add(commit.parent2());
        }

        // Walk b's history until we hit one of a's ancestors
        queue.add(b);
        java.util.Set<String> visited = new java.util.HashSet<>();

        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current == null || visited.contains(current)) continue;
            visited.add(current);
            if (ancestorsA.contains(current)) return current;

            CommitParser.CommitObject commit = CommitParser.parse(
                    ObjectReader.read(current));
            if (commit.parent() != null)  queue.add(commit.parent());
            if (commit.parent2() != null) queue.add(commit.parent2());
        }

        return null;
    }

    private static String getParent(String commitHash) throws Exception {
        String raw = ObjectReader.read(commitHash);
        CommitParser.CommitObject commit = CommitParser.parse(raw);
        return commit.parent();
    }

    private static Map<String, String> getFiles(String commitHash) throws Exception {
        String raw = ObjectReader.read(commitHash);
        CommitParser.CommitObject commit = CommitParser.parse(raw);
        return TreeFlattener.flatten(commit.tree(), "");
    }

    private static void fastForward(String branch, String targetHash) throws Exception {
        Path ref = Path.of(".git", "refs", "heads", branch);
        Files.writeString(ref, targetHash + "\n");

        // Restore working directory to target commit
        String raw = ObjectReader.read(targetHash);
        CommitParser.CommitObject commit = CommitParser.parse(raw);
        TreeReader.restore(commit.tree(), Path.of("."));
    }

    private static void updateRef(String branch, String hash) throws Exception {
        Path ref = Path.of(".git", "refs", "heads", branch);
        Files.writeString(ref, hash + "\n");
    }

    private static String resolveBranch() throws Exception {
        Path head = Path.of(".git", "HEAD");
        String headContent = Files.readString(head).trim();
        if (headContent.startsWith("ref: ")) {
            return Path.of(headContent.substring(5).trim())
                    .getFileName().toString();
        }
        throw new IllegalStateException("Detached HEAD — cannot merge");
    }

    private static String resolveRef(String branch) throws Exception {
        Path ref = Path.of(".git", "refs", "heads", branch);
        return Files.readString(ref).trim();
    }
}