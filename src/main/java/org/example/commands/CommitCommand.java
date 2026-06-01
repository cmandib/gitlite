package org.example.commands;

import org.example.storage.CommitWriter;
import org.example.storage.TreeWriter;

import java.nio.file.Files;
import java.nio.file.Path;

public class CommitCommand {

    public static void execute(String[] args) throws Exception {
        String message = null;

        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("-m") && i + 1 < args.length) {
                message = args[i + 1];
                i++;
            }
        }

        if (message == null) {
            throw new IllegalArgumentException("Usage: commit -m <message>");
        }

        // Snapshot working directory as a tree
        String treeHash = TreeWriter.write(Path.of("."));

        // Build and store the commit object
        String commitHash = CommitWriter.write(treeHash, message);

        // Advance the branch ref to point to new commit
        updateHead(commitHash);

        System.out.println("[commit " + commitHash + "] " + message);
    }

    private static void updateHead(String commitHash) throws Exception {
        Path head = Path.of(".git", "HEAD");
        String headContent = Files.readString(head).trim();

        if (headContent.startsWith("ref: ")) {
            String refPath = headContent.substring(5).trim();
            Path ref = Path.of(".git").resolve(refPath);
            Files.createDirectories(ref.getParent());
            Files.writeString(ref, commitHash + "\n");
        } else {
            // detached HEAD — write hash directly
            Files.writeString(head, commitHash + "\n");
        }
    }
}