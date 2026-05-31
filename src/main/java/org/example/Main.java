package org.example;

import org.example.commands.InitCommand;

import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) throws Exception {

        System.out.println("Working directory: " +
                System.getProperty("user.dir"));

        if (args.length == 0) {
            System.out.println("Usage: gitlite <command>");
            return;
        }

        switch (args[0]) {
            case "init":
                InitCommand.execute(Paths.get("."));
                break;

            default:
                System.out.println("Unknown command: " + args[0]);
        }
    }
}