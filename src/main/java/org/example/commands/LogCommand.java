package org.example.commands;

import org.example.storage.CommitParser;
import org.example.storage.CommitParser.CommitObject;
import org.example.storage.ObjectReader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class LogCommand {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("EEE MMM dd HH:mm:ss yyyy Z", Locale.ENGLISH);

    public static void execute() throws Exception {
        String hash = resolveHead();

        if (hash == null) {
            System.out.println("No commits yet.");
            return;
        }

        while (hash != null) {
            String raw = ObjectReader.read(hash);
            CommitObject commit = CommitParser.parse(raw);

            System.out.println("commit " + hash);
            System.out.println("Author: " + commit.author());
            System.out.println("Date:   " + formatDate(commit.author()));
            System.out.println();
            System.out.println("    " + commit.message());
            System.out.println();

            hash = commit.parent();
        }
    }

    private static String resolveHead() throws Exception {
        Path head = Path.of(".git", "HEAD");
        String headContent = Files.readString(head).trim();

        if (headContent.startsWith("ref: ")) {
            String refPath = headContent.substring(5).trim();
            Path ref = Path.of(".git").resolve(refPath);

            if (!Files.exists(ref)) {
                return null;
            }

            return Files.readString(ref).trim();
        }

        return headContent;
    }

    private static String formatDate(String author) {
        // author format: "name <email> <epoch> <timezone>"
        String[] parts = author.split(" ");
        long epoch = Long.parseLong(parts[parts.length - 2]);
        String tz = parts[parts.length - 1];

        ZoneOffset offset = ZoneOffset.of(tz);
        ZonedDateTime dt = ZonedDateTime.ofInstant(Instant.ofEpochSecond(epoch), offset);

        return FORMATTER.format(dt);
    }
}