package org.example.storage;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.zip.Deflater;

public class ObjectStore {

    public static String store(byte[] content, boolean write) throws Exception {

        // Build Git blob object: "blob <size>\0<content>"
        ByteArrayOutputStream full = new ByteArrayOutputStream();

        byte[] header = ("blob " + content.length)
                .getBytes(StandardCharsets.UTF_8);

        full.write(header);
        full.write(0);
        full.write(content);

        byte[] raw = full.toByteArray();

        // SHA-1 hash of raw object
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] digest = md.digest(raw);
        String hash = toHex(digest);

        if (!write) {
            return hash;
        }

        byte[] compressed = compress(raw);

        String dir = hash.substring(0, 2);
        String file = hash.substring(2);

        Path objectDir = Path.of(".git", "objects", dir);
        Files.createDirectories(objectDir);

        Path objectPath = objectDir.resolve(file);

        try (FileOutputStream out = new FileOutputStream(objectPath.toFile())) {
            out.write(compressed);
        }

        return hash;
    }

    private static byte[] compress(byte[] data) {
        Deflater deflater = new Deflater();
        deflater.setInput(data);
        deflater.finish();

        byte[] buffer = new byte[1024];
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            out.write(buffer, 0, count);
        }

        deflater.end();
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