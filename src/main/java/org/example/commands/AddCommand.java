package org.example.commands;

import org.example.storage.Index;
import org.example.storage.ObjectStore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class AddCommand {

    public static void execute(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: add <file>");
        }

        String filePath = args[1];
        Path path = Path.of(filePath);

        if (!Files.exists(path)) {
            throw new IllegalArgumentException("File not found: " + filePath);
        }

        byte[] content = Files.readAllBytes(path);
        String hash = ObjectStore.store(content, true);

        Map<String, String> index = Index.read();
        index.put(filePath, hash);
        Index.write(index);

        System.out.println("Added: " + filePath);
    }
}