package org.example.storage;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.InflaterInputStream;

public class TreeReader {

    public static void restore(String treeHash, Path targetDir) throws Exception {
        byte[] raw = readRawObject(treeHash);

        // Strip "tree <size>\0" header
        int nullIndex = -1;
        for (int i = 0; i < raw.length; i++) {
            if (raw[i] == 0) {
                nullIndex = i;
                break;
            }
        }

        if (nullIndex == -1) {
            throw new IllegalStateException("Invalid tree object");
        }

        // Parse entries
        int pos = nullIndex + 1;

        while (pos < raw.length) {
            // Read mode + name: "<mode> <name>\0"
            int entryNullIndex = pos;
            while (raw[entryNullIndex] != 0) entryNullIndex++;

            String modeAndName = new String(raw, pos, entryNullIndex - pos, StandardCharsets.UTF_8);
            int spaceIndex = modeAndName.indexOf(' ');
            String mode = modeAndName.substring(0, spaceIndex);
            String name = modeAndName.substring(spaceIndex + 1);

            pos = entryNullIndex + 1;

            // Read 20 raw bytes of SHA-1
            byte[] hashBytes = new byte[20];
            System.arraycopy(raw, pos, hashBytes, 0, 20);
            pos += 20;

            String hash = toHex(hashBytes);

            if (mode.equals("40000")) {
                // directory — recurse
                Path subDir = targetDir.resolve(name);
                Files.createDirectories(subDir);
                restore(hash, subDir);
            } else {
                // file — read blob and write to disk
                String content = ObjectReader.read(hash);
                Path filePath = targetDir.resolve(name);
                Files.writeString(filePath, content);
            }
        }
    }

    public static byte[] readRawObject(String hash) throws Exception {
        String dir = hash.substring(0, 2);
        String file = hash.substring(2);

        Path objectPath = Path.of(".git", "objects", dir, file);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (InflaterInputStream in =
                     new InflaterInputStream(Files.newInputStream(objectPath))) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }

        return out.toByteArray();
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}