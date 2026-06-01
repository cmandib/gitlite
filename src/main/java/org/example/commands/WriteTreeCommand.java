package org.example.commands;

import org.example.storage.TreeWriter;

import java.nio.file.Path;

public class WriteTreeCommand {

    public static void execute() throws Exception {
        String hash = TreeWriter.write(Path.of("."));
        System.out.println(hash);
    }
}