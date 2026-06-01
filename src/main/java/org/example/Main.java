package org.example;

import org.example.commands.*;

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

            case "write-tree":
                WriteTreeCommand.execute();
                break;

            case "commit":
                CommitCommand.execute(args);
                break;

            case "log":
                LogCommand.execute();
                break;

            case "branch":
                BranchCommand.execute(args);
                break;

            case "checkout":
                CheckoutCommand.execute(args);
                break;

            default:
                System.out.println("Unknown command: " + args[0]);
        }
    }
}