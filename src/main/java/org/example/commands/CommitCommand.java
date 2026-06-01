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

        // Check for merge in progress
        Path mergeHead = Path.of(".git", "MERGE_HEAD");
        boolean isMergeCommit = Files.exists(mergeHead);

        if (message == null) {
            if (isMergeCommit) {
                // Use the saved merge message
                message = Files.readString(Path.of(".git", "MERGE_MSG")).trim();
            } else {
                throw new IllegalArgumentException("Usage: commit -m <message>");
            }
        }

        Map<String, String> index = Index.read();

        if (index.isEmpty()) {
            System.out.println("Nothing to commit — stage files with add first");
            return;
        }

        String treeHash = TreeWriter.writeFromIndex(index);
        String commitHash;

        if (isMergeCommit) {
            String parent2 = Files.readString(mergeHead).trim();
            commitHash = CommitWriter.writeMergeCommit(treeHash,
                    resolveCurrentHash(), parent2, message);
            // Clean up merge state
            Files.delete(mergeHead);
            Files.deleteIfExists(Path.of(".git", "MERGE_MSG"));
        } else {
            commitHash = CommitWriter.write(treeHash, message);
        }

        updateHead(commitHash);
        Index.write(new java.util.LinkedHashMap<>());

        System.out.println("[commit " + commitHash + "] " + message);
    }

    private static String resolveCurrentHash() throws Exception {
        Path head = Path.of(".git", "HEAD");
        String headContent = Files.readString(head).trim();
        if (headContent.startsWith("ref: ")) {
            String refPath = headContent.substring(5).trim();
            return Files.readString(Path.of(".git").resolve(refPath)).trim();
        }
        return headContent;
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