package org.example.commands;

import org.example.storage.ObjectStore;

import java.nio.file.Files;
import java.nio.file.Path;

public class HashObjectCommand {

    public static void execute(String[] args) throws Exception {

        boolean write = false;
        String filePath = null;

        // args[0] is "hash-object", so start at 1
        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("-w")) {
                write = true;
            } else {
                filePath = args[i];
            }
        }

        if (filePath == null) {
            throw new IllegalArgumentException("No file provided");
        }

        Path path = Path.of(filePath);
        byte[] content = Files.readAllBytes(path);

        String hash = ObjectStore.store(content, write);

        System.out.println(hash);
    }
}