package org.example.storage;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

public class Index {

    private static final Path INDEX_PATH = Path.of(".git", "index");
    private static final byte[] MAGIC = {'D', 'I', 'R', 'C'};
    private static final int VERSION = 2;

    public static Map<String, String> read() throws Exception {
        Map<String, String> entries = new LinkedHashMap<>();

        if (!Files.exists(INDEX_PATH)) {
            return entries;
        }

        byte[] data = Files.readAllBytes(INDEX_PATH);
        ByteBuffer buf = ByteBuffer.wrap(data, 0, data.length - 20)
                .order(ByteOrder.BIG_ENDIAN);

        byte[] magic = new byte[4];
        buf.get(magic);
        if (magic[0] != 'D' || magic[1] != 'I' ||
                magic[2] != 'R' || magic[3] != 'C') {
            throw new IllegalStateException("Invalid index file: bad magic");
        }

        int version = buf.getInt();
        if (version != 2) {
            throw new IllegalStateException("Unsupported index version: " + version);
        }

        int entryCount = buf.getInt();

        for (int i = 0; i < entryCount; i++) {
            int entryStart = buf.position();

            buf.position(buf.position() + 40);

            byte[] hashBytes = new byte[20];
            buf.get(hashBytes);
            String hash = toHex(hashBytes);

            short flags = buf.getShort();
            int nameLength = flags & 0xFFF;

            byte[] nameBytes = new byte[nameLength];
            buf.get(nameBytes);
            String name = new String(nameBytes, StandardCharsets.UTF_8);

            int entrySize = 62 + nameLength + 1;
            int paddedSize = ((entrySize + 7) / 8) * 8;
            buf.position(entryStart + paddedSize);

            entries.put(name, hash);
        }

        return entries;
    }

    public static void write(Map<String, String> entries) throws Exception {
        ByteArrayOutputStream body = new ByteArrayOutputStream();

        body.write(MAGIC);
        body.write(intToBytes(VERSION));
        body.write(intToBytes(entries.size()));

        for (Map.Entry<String, String> entry : entries.entrySet()) {
            String name = entry.getKey();
            String hash = entry.getValue();

            Path filePath = Path.of(name);
            BasicFileAttributes attrs = Files.exists(filePath)
                    ? Files.readAttributes(filePath, BasicFileAttributes.class)
                    : null;

            long ctimeSec  = attrs != null ? attrs.creationTime().toInstant().getEpochSecond() : 0;
            long ctimeNano = attrs != null ? attrs.creationTime().toInstant().getNano() : 0;
            long mtimeSec  = attrs != null ? attrs.lastModifiedTime().toInstant().getEpochSecond() : 0;
            long mtimeNano = attrs != null ? attrs.lastModifiedTime().toInstant().getNano() : 0;
            long size      = attrs != null ? attrs.size() : 0;

            body.write(intToBytes((int) ctimeSec));
            body.write(intToBytes((int) ctimeNano));
            body.write(intToBytes((int) mtimeSec));
            body.write(intToBytes((int) mtimeNano));
            body.write(intToBytes(0));
            body.write(intToBytes(0));
            body.write(intToBytes(0x81A4));
            body.write(intToBytes(0));
            body.write(intToBytes(0));
            body.write(intToBytes((int) size));

            body.write(hexToBytes(hash));

            byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
            int nameLen = Math.min(nameBytes.length, 0xFFF);
            body.write(shortToBytes((short) nameLen));

            body.write(nameBytes);
            body.write(0);

            int entrySize = 62 + nameBytes.length + 1;
            int paddedSize = ((entrySize + 7) / 8) * 8;
            int padding = paddedSize - entrySize;
            for (int i = 0; i < padding; i++) {
                body.write(0);
            }
        }

        byte[] bodyBytes = body.toByteArray();

        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] checksum = md.digest(bodyBytes);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(bodyBytes);
        out.write(checksum);

        Files.write(INDEX_PATH, out.toByteArray());
    }

    private static byte[] intToBytes(int value) {
        return ByteBuffer.allocate(4)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(value)
                .array();
    }

    private static byte[] shortToBytes(short value) {
        return ByteBuffer.allocate(2)
                .order(ByteOrder.BIG_ENDIAN)
                .putShort(value)
                .array();
    }

    private static byte[] hexToBytes(String hex) {
        byte[] result = new byte[20];
        for (int i = 0; i < 20; i++) {
            result[i] = (byte) Integer.parseInt(
                    hex.substring(i * 2, i * 2 + 2), 16);
        }
        return result;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}