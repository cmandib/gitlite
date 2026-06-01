package org.example.storage;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class TreeFlattener {

    public static Map<String, String> flatten(String treeHash, String prefix) throws Exception {
        Map<String, String> result = new HashMap<>();
        byte[] raw = TreeReader.readRawObject(treeHash);

        // Strip "tree <size>\0" header
        int nullIndex = -1;
        for (int i = 0; i < raw.length; i++) {
            if (raw[i] == 0) { nullIndex = i; break; }
        }

        int pos = nullIndex + 1;

        while (pos < raw.length) {
            // Read "<mode> <name>\0"
            int entryNull = pos;
            while (raw[entryNull] != 0) entryNull++;

            String modeAndName = new String(raw, pos, entryNull - pos, StandardCharsets.UTF_8);
            int spaceIndex = modeAndName.indexOf(' ');
            String mode = modeAndName.substring(0, spaceIndex);
            String name = modeAndName.substring(spaceIndex + 1);

            pos = entryNull + 1;

            // Read 20 raw bytes of SHA-1
            byte[] hashBytes = new byte[20];
            System.arraycopy(raw, pos, hashBytes, 0, 20);
            pos += 20;

            String hash = toHex(hashBytes);
            String fullPath = prefix.isEmpty() ? name : prefix + "/" + name;

            if (mode.equals("40000")) {
                // recurse into subtree
                result.putAll(flatten(hash, fullPath));
            } else {
                result.put(fullPath, hash);
            }
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