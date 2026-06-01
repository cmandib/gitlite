package org.example.commands;

import org.example.storage.CommitParser;
import org.example.storage.DiffEngine;
import org.example.storage.ObjectReader;
import org.example.storage.TreeReader;
import org.example.storage.TreeFlattener;

import java.util.Map;

public class DiffCommand {

    public static void execute(String[] args) throws Exception {
        if (args.length < 3) {
            throw new IllegalArgumentException(
                    "Usage: diff <commit1> <commit2>");
        }

        String hash1 = args[1];
        String hash2 = args[2];

        // Resolve trees from commits
        String tree1 = getTree(hash1);
        String tree2 = getTree(hash2);

        // Flatten both trees into filename -> blob hash maps
        Map<String, String> files1 = TreeFlattener.flatten(tree1, "");
        Map<String, String> files2 = TreeFlattener.flatten(tree2, "");

        DiffEngine.diff(files1, files2);
    }

    private static String getTree(String commitHash) throws Exception {
        String raw = ObjectReader.read(commitHash);
        CommitParser.CommitObject commit = CommitParser.parse(raw);
        return commit.tree();
    }
}