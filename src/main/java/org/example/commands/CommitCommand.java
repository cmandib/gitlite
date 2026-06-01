package org.example.commands;

import org.example.storage.CommitWriter;
import org.example.storage.Index;
import org.example.storage.TreeWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

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

        Map<String, String> index = Index.read();

        if (index.isEmpty()) {
            System.out.println("Nothing to commit — stage files with add first");
            return;
        }

        // Build tree from index instead of scanning working directory
        String treeHash = TreeWriter.writeFromIndex(index);

        String commitHash = CommitWriter.write(treeHash, message);

        updateHead(commitHash);

        // Clear index after commit
        Index.write(new java.util.LinkedHashMap<>());

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
            Files.writeString(head, commitHash + "\n");
        }
    }
}