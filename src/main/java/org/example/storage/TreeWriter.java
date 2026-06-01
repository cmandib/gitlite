package org.example.storage;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class TreeWriter {

    public static String write(Path dir) throws Exception {
        List<Path> entries = new ArrayList<>();

        try (var stream = Files.list(dir)) {
            stream
                    .filter(p -> !p.getFileName().toString().equals(".git"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .forEach(entries::add);
        }

        ByteArrayOutputStream treeBody = new ByteArrayOutputStream();

        for (Path entry : entries) {
            String name = entry.getFileName().toString();
            String mode;
            String hash;

            if (Files.isDirectory(entry)) {
                mode = "40000";
                hash = write(entry); // recurse
            } else {
                mode = "100644";
                byte[] content = Files.readAllBytes(entry);
                hash = ObjectStore.store(content, true);
            }

            // "<mode> <name>\0<20-byte raw SHA-1>"
            String entryHeader = mode + " " + name;
            treeBody.write(entryHeader.getBytes(StandardCharsets.UTF_8));
            treeBody.write(0);
            treeBody.write(hexToBytes(hash));
        }

        byte[] body = treeBody.toByteArray();

        // Build full tree object: "tree <size>\0<body>"
        ByteArrayOutputStream full = new ByteArrayOutputStream();
        full.write(("tree " + body.length).getBytes(StandardCharsets.UTF_8));
        full.write(0);
        full.write(body);

        return ObjectStore.storeRaw(full.toByteArray());
    }

    public static String writeFromIndex(Map<String, String> index) throws Exception {
        ByteArrayOutputStream treeBody = new ByteArrayOutputStream();

        // Sort entries by filename for consistent ordering
        List<Map.Entry<String, String>> entries = new ArrayList<>(index.entrySet());
        entries.sort(Map.Entry.comparingByKey());

        for (Map.Entry<String, String> entry : entries) {
            String name = entry.getKey();
            String hash = entry.getValue();

            String entryHeader = "100644 " + name;
            treeBody.write(entryHeader.getBytes(StandardCharsets.UTF_8));
            treeBody.write(0);
            treeBody.write(hexToBytes(hash));
        }

        byte[] body = treeBody.toByteArray();

        ByteArrayOutputStream full = new ByteArrayOutputStream();
        full.write(("tree " + body.length).getBytes(StandardCharsets.UTF_8));
        full.write(0);
        full.write(body);

        return ObjectStore.storeRaw(full.toByteArray());
    }

    private static byte[] hexToBytes(String hex) {
        byte[] result = new byte[20];
        for (int i = 0; i < 20; i++) {
            result[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return result;
    }
}