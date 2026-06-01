package org.example.commands;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class BranchCommand {

    public static void execute(String[] args) throws Exception {
        if (args.length < 2) {
            list();
        } else {
            create(args[1]);
        }
    }

    private static void list() throws Exception {
        Path headsDir = Path.of(".git", "refs", "heads");
        String current = resolveCurrent();

        List<String> branches = Files.list(headsDir)
                .map(p -> p.getFileName().toString())
                .sorted()
                .collect(Collectors.toList());

        for (String branch : branches) {
            if (branch.equals(current)) {
                System.out.println("* " + branch);
            } else {
                System.out.println("  " + branch);
            }
        }
    }

    private static void create(String name) throws Exception {
        // new branch points to current HEAD commit
        Path head = Path.of(".git", "HEAD");
        String headContent = Files.readString(head).trim();

        String commitHash;

        if (headContent.startsWith("ref: ")) {
            String refPath = headContent.substring(5).trim();
            Path ref = Path.of(".git").resolve(refPath);

            if (!Files.exists(ref)) {
                throw new IllegalStateException(
                        "No commits yet — cannot create branch");
            }

            commitHash = Files.readString(ref).trim();
        } else {
            commitHash = headContent;
        }

        Path newBranch = Path.of(".git", "refs", "heads", name);

        if (Files.exists(newBranch)) {
            throw new IllegalStateException(
                    "Branch already exists: " + name);
        }

        Files.writeString(newBranch, commitHash + "\n");
        System.out.println("Created branch: " + name);
    }

    private static String resolveCurrent() throws Exception {
        Path head = Path.of(".git", "HEAD");
        String headContent = Files.readString(head).trim();

        if (headContent.startsWith("ref: ")) {
            // extract just the branch name from "ref: refs/heads/main"
            String refPath = headContent.substring(5).trim();
            return Path.of(refPath).getFileName().toString();
        }

        // detached HEAD — no current branch
        return null;
    }
}