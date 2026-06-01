package org.example.config;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class GitConfig {

    public static String get(String key) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("git", "config", key);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String value = reader.readLine();
            process.waitFor();
            if (value == null || value.isBlank()) {
                throw new IllegalStateException(
                        "Git config value not found for key: " + key);
            }
            return value.trim();
        }
    }
}