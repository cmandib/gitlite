package org.example.commands;

import org.example.storage.ObjectStore;
import org.example.storage.CommitParser;
import org.example.storage.ObjectReader;
import org.example.storage.TreeFlattener;

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

        // Walk working directory
        Map<String, String> working = new TreeMap<>();
        walkDir(Path.of("."), "", working);

        List<String> modified = new ArrayList<>();
        List<String> deleted = new ArrayList<>();
        List<String> untracked = new ArrayList<>();

        // Check committed files against working directory
        for (Map.Entry<String, String> entry : committed.entrySet()) {
            String file = entry.getKey();
            String committedHash = entry.getValue();

            if (!working.containsKey(file)) {
                deleted.add(file);
            } else if (!working.get(file).equals(committedHash)) {
                modified.add(file);
            }
        }

        // Check working files against committed
        for (String file : working.keySet()) {
            if (!committed.containsKey(file)) {
                untracked.add(file);
            }
        }

        // Print results
        if (modified.isEmpty() && deleted.isEmpty()) {
            System.out.println("Nothing to commit, working tree clean");
        } else {
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