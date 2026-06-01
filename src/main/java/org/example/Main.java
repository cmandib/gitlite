package org.example;

import org.example.commands.CatFileCommand;
import org.example.commands.HashObjectCommand;
import org.example.commands.InitCommand;

import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            System.out.println("Usage: gitlite <command>");
            return;
        }

        switch (args[0]) {

            case "init":
                InitCommand.execute(Paths.get("."));
                break;

            case "hash-object":
                HashObjectCommand.execute(args);
                break;

            case "cat-file":
                CatFileCommand.execute(args);
                break;

            default:
                System.out.println("Unknown command: " + args[0]);
        }
    }
}