package org.example.commands;

import org.example.storage.ObjectReader;

public class CatFileCommand {

    public static void execute(String[] args) throws Exception {

        if (args.length < 3) {
            throw new IllegalArgumentException(
                    "Usage: cat-file -p <hash>");
        }

        String option = args[1];
        String hash = args[2];

        if (!option.equals("-p")) {
            throw new IllegalArgumentException(
                    "Only -p supported");
        }

        String content = ObjectReader.read(hash);

        System.out.println(content);
    }
}