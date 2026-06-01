package org.example.storage;

import org.example.config.GitConfig;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class CommitWriter {

    public static String write(String treeHash, String message) throws Exception {
        String parent = resolveParent();
        String author = buildIdentity();

        StringBuilder sb = new StringBuilder();
        sb.append("tree ").append(treeHash).append("\n");

        if (parent != null) {
            sb.append("parent ").append(parent).append("\n");
        }

        sb.append("author ").append(author).append("\n");
        sb.append("committer ").append(author).append("\n");
        sb.append("\n");
        sb.append(message).append("\n");

        byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);

        ByteArrayOutputStream full = new ByteArrayOutputStream();
        full.write(("commit " + body.length).getBytes(StandardCharsets.UTF_8));
        full.write(0);
        full.write(body);

        return ObjectStore.storeRaw(full.toByteArray());
    }

    public static String writeMergeCommit(
            String treeHash,
            String parent1,
            String parent2,
            String message) throws Exception {

        String author = buildIdentity();

        StringBuilder sb = new StringBuilder();
        sb.append("tree ").append(treeHash).append("\n");
        sb.append("parent ").append(parent1).append("\n");
        sb.append("parent ").append(parent2).append("\n");
        sb.append("author ").append(author).append("\n");
        sb.append("committer ").append(author).append("\n");
        sb.append("\n");
        sb.append(message).append("\n");

        byte[] body = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

        java.io.ByteArrayOutputStream full = new java.io.ByteArrayOutputStream();
        full.write(("commit " + body.length).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        full.write(0);
        full.write(body);

        return ObjectStore.storeRaw(full.toByteArray());
    }

    private static String resolveParent() throws Exception {
        Path head = Path.of(".git", "HEAD");
        String headContent = Files.readString(head).trim();

        // HEAD is a symbolic ref: "ref: refs/heads/main"
        if (headContent.startsWith("ref: ")) {
            String refPath = headContent.substring(5).trim();
            Path ref = Path.of(".git").resolve(refPath);

            if (!Files.exists(ref)) {
                return null; // first commit, no parent
            }

            return Files.readString(ref).trim();
        }

        // detached HEAD — HEAD contains the hash directly
        return headContent;
    }

    private static String buildIdentity() throws Exception {
        String name = GitConfig.get("user.name");
        String email = GitConfig.get("user.email");

        long epochSeconds = Instant.now().getEpochSecond();

        ZoneOffset offset = ZoneId.systemDefault()
                .getRules()
                .getOffset(Instant.now());

        // Format: "+0545" for Kathmandu
        String timezone = DateTimeFormatter
                .ofPattern("xx")
                .format(offset);

        return name + " <" + email + "> " + epochSeconds + " " + timezone;
    }
}