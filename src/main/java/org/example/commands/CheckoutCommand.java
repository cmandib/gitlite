package org.example.commands;

import org.example.storage.CommitParser;
import org.example.storage.ObjectReader;
import org.example.storage.TreeReader;

import java.nio.file.Files;
import java.nio.file.Path;

public class CheckoutCommand {

    public static void execute(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: checkout <branch>");
        }

        String branch = args[1];
        Path branchRef = Path.of(".git", "refs", "heads", branch);

        if (!Files.exists(branchRef)) {
            throw new IllegalStateException("Branch not found: " + branch);
        }

        // Read commit hash from branch ref
        String commitHash = Files.readString(branchRef).trim();

        // Read and parse the commit object
        String rawCommit = ObjectReader.read(commitHash);
        CommitParser.CommitObject commit = CommitParser.parse(rawCommit);

        // Restore working directory from the commit's tree
        TreeReader.restore(commit.tree(), Path.of("."));

        // Update HEAD to point to the new branch
        Files.writeString(
                Path.of(".git", "HEAD"),
                "ref: refs/heads/" + branch + "\n"
        );

        System.out.println("Switched to branch: " + branch);
    }
}