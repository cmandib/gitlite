package org.example.storage;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.InflaterInputStream;

public class ObjectReader {

    public static String read(String hash) throws Exception {

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

        byte[] raw = out.toByteArray();

        int nullIndex = -1;
        for (int i = 0; i < raw.length; i++) {
            if (raw[i] == 0) {
                nullIndex = i;
                break;
            }
        }

        if (nullIndex == -1) {
            throw new IllegalStateException("Invalid git object: no null byte found");
        }

        return new String(
                raw,
                nullIndex + 1,
                raw.length - nullIndex - 1,
                StandardCharsets.UTF_8
        );
    }
}