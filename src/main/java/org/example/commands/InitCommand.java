package org.example.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class InitCommand {

    public static void execute(Path repo) throws IOException {

        Path gitDir = repo.resolve(".git");

        Files.createDirectories(gitDir.resolve("objects"));
        Files.createDirectories(gitDir.resolve("refs/heads"));
        Files.createDirectories(gitDir.resolve("refs/tags"));

        Files.writeString(
                gitDir.resolve("HEAD"),
                "ref: refs/heads/main\n"
        );

        System.out.println("Initialized empty Git repository");
    }
}