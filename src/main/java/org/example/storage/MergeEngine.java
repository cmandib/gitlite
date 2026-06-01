package org.example.storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class MergeEngine {

    public static boolean merge(
            Map<String, String> ancestor,
            Map<String, String> current,
            Map<String, String> target) throws Exception {

        TreeSet<String> allFiles = new TreeSet<>();
        allFiles.addAll(ancestor.keySet());
        allFiles.addAll(current.keySet());
        allFiles.addAll(target.keySet());

        boolean hasConflicts = false;

        for (String file : allFiles) {
            String ancestorHash = ancestor.get(file);
            String currentHash  = current.get(file);
            String targetHash   = target.get(file);

            boolean inAncestor = ancestorHash != null;
            boolean inCurrent  = currentHash != null;
            boolean inTarget   = targetHash != null;

            // Only in current — keep, nothing to write
            if (!inAncestor && inCurrent && !inTarget) continue;

            // Only in target — take it
            if (!inAncestor && !inCurrent && inTarget) {
                writeFile(file, ObjectReader.read(targetHash));
                System.out.println("Added:    " + file);
                continue;
            }

            // Deleted in both branches
            if (inAncestor && !inCurrent && !inTarget) continue;

            // Deleted in current, still in target — conflict
            if (inAncestor && !inCurrent && inTarget) {
                System.out.println("CONFLICT (delete/modify): " + file);
                hasConflicts = true;
                continue;
            }

            // Deleted in target, still in current — conflict
            if (inAncestor && inCurrent && !inTarget) {
                System.out.println("CONFLICT (modify/delete): " + file);
                hasConflicts = true;
                continue;
            }

            // Both branches have the file — check what changed
            boolean currentChanged = !currentHash.equals(
                    ancestorHash != null ? ancestorHash : "");
            boolean targetChanged  = !targetHash.equals(
                    ancestorHash != null ? ancestorHash : "");

            if (!currentChanged && !targetChanged) {
                // Neither changed — nothing to do
                continue;
            }

            if (currentChanged && !targetChanged) {
                // Only current changed — keep current, nothing to write
                continue;
            }

            if (!currentChanged && targetChanged) {
                // Only target changed — take target
                writeFile(file, ObjectReader.read(targetHash));
                System.out.println("Updated:  " + file);
                continue;
            }

            // Both changed
            if (currentHash.equals(targetHash)) {
                // Same result — no conflict
                continue;
            }

            // Both changed differently — real conflict
            String currentContent = ObjectReader.read(currentHash);
            String targetContent  = ObjectReader.read(targetHash);
            writeFile(file, buildConflictMarkers(currentContent, targetContent));
            System.out.println("CONFLICT: " + file);
            hasConflicts = true;
        }

        return hasConflicts;
    }

    private static void writeFile(String path, String content) throws Exception {
        Path filePath = Path.of(path);
        Files.createDirectories(filePath.getParent() != null
                ? filePath.getParent() : Path.of("."));
        Files.writeString(filePath, content);
    }

    private static String buildConflictMarkers(String current, String target) {
        List<String> currentLines = splitLines(current);
        List<String> targetLines  = splitLines(target);

        StringBuilder sb = new StringBuilder();
        sb.append("<<<<<<< HEAD\n");
        for (String line : currentLines) sb.append(line).append("\n");
        sb.append("=======\n");
        for (String line : targetLines) sb.append(line).append("\n");
        sb.append(">>>>>>> MERGE\n");
        return sb.toString();
    }

    private static List<String> splitLines(String content) {
        List<String> lines = new ArrayList<>();
        if (content == null || content.isEmpty()) return lines;
        for (String line : content.split("\n", -1)) lines.add(line);
        if (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }
        return lines;
    }
}