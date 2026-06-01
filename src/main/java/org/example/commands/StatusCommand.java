package org.example.commands;

import org.example.storage.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class StatusCommand {

    public static void execute() throws Exception {
        String branch = resolveBranch();
        System.out.println("On branch " + branch);
        System.out.println();

        Map<String, String> committed = getCommittedFiles();
        Map<String, String> index = Index.read();

        Map<String, String> working = new TreeMap<>();
        walkDir(Path.of("."), "", working);

        Map<String, String> baseline = new TreeMap<>(committed);
        baseline.putAll(index);

        List<String> modified = new ArrayList<>();
        List<String> deleted = new ArrayList<>();
        List<String> untracked = new ArrayList<>();

        for (Map.Entry<String, String> entry : baseline.entrySet()) {
            String file = entry.getKey();
            String baseHash = entry.getValue();

            if (!working.containsKey(file)) {
                deleted.add(file);
            } else if (!working.get(file).equals(baseHash)) {
                modified.add(file);
            }
        }

        for (String file : working.keySet()) {
            if (!baseline.containsKey(file)) {
                untracked.add(file);
            }
        }

        List<String> staged = new ArrayList<>();
        for (Map.Entry<String, String> entry : index.entrySet()) {
            String file = entry.getKey();
            String indexHash = entry.getValue();
            String committedHash = committed.get(file);

            if (!indexHash.equals(committedHash != null ? committedHash : "")) {
                staged.add(file);
            }
        }

        // Print
        if (modified.isEmpty() && deleted.isEmpty() && staged.isEmpty() && untracked.isEmpty()) {
            System.out.println("Nothing to commit, working tree clean");
            return;
        }

        if (!staged.isEmpty()) {
            System.out.println("Changes to be committed:");
            for (String f : staged) {
                System.out.println("    staged:     " + f);
            }
            System.out.println();
        }

        if (!modified.isEmpty() || !deleted.isEmpty()) {
            System.out.println("Changes not staged for commit:");
            for (String f : modified) {
                System.out.println("    modified:   " + f);
            }
            for (String f : deleted) {
                System.out.println("    deleted:    " + f);
            }
            System.out.println();
        }

        if (!untracked.isEmpty()) {
            System.out.println("Untracked files:");
            for (String f : untracked) {
                System.out.println("    " + f);
            }
            System.out.println();
        }
    }

    private static Map<String, String> getCommittedFiles() throws Exception {
        Path head = Path.of(".git", "HEAD");
        String headContent = Files.readString(head).trim();

        if (headContent.startsWith("ref: ")) {
            String refPath = headContent.substring(5).trim();
            Path ref = Path.of(".git").resolve(refPath);

            if (!Files.exists(ref)) {
                return new TreeMap<>(); // no commits yet
            }

            String commitHash = Files.readString(ref).trim();
            String raw = ObjectReader.read(commitHash);
            CommitParser.CommitObject commit = CommitParser.parse(raw);
            return TreeFlattener.flatten(commit.tree(), "");
        }

        return new TreeMap<>();
    }

    private static void walkDir(Path dir, String prefix,
                                Map<String, String> result) throws Exception {
        try (var stream = Files.list(dir)) {
            for (Path entry : stream.sorted().toList()) {
                String name = entry.getFileName().toString();

                if (name.equals(".git")) continue;

                String fullPath = prefix.isEmpty() ? name : prefix + "/" + name;

                if (Files.isDirectory(entry)) {
                    walkDir(entry, fullPath, result);
                } else {
                    byte[] content = Files.readAllBytes(entry);
                    String hash = ObjectStore.store(content, false);
                    result.put(fullPath, hash);
                }
            }
        }
    }

    private static String resolveBranch() throws Exception {
        Path head = Path.of(".git", "HEAD");
        String headContent = Files.readString(head).trim();

        if (headContent.startsWith("ref: ")) {
            return Path.of(headContent.substring(5).trim())
                    .getFileName().toString();
        }

        return headContent.substring(0, 7) + "..."; // detached HEAD
    }
}