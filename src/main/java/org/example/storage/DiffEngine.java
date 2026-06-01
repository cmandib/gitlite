package org.example.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class DiffEngine {

    public static void diff(Map<String, String> files1,
                            Map<String, String> files2) throws Exception {

        // All filenames across both commits
        TreeSet<String> allFiles = new TreeSet<>();
        allFiles.addAll(files1.keySet());
        allFiles.addAll(files2.keySet());

        for (String file : allFiles) {
            boolean inOld = files1.containsKey(file);
            boolean inNew = files2.containsKey(file);

            if (inOld && !inNew) {
                // deleted file
                String content = ObjectReader.read(files1.get(file));
                System.out.println("diff --git a/" + file + " b/" + file);
                System.out.println("deleted file mode 100644");
                System.out.println("--- a/" + file);
                System.out.println("+++ /dev/null");
                printDiff(content, "");

            } else if (!inOld && inNew) {
                // added file
                String content = ObjectReader.read(files2.get(file));
                System.out.println("diff --git a/" + file + " b/" + file);
                System.out.println("new file mode 100644");
                System.out.println("--- /dev/null");
                System.out.println("+++ b/" + file);
                printDiff("", content);

            } else if (!files1.get(file).equals(files2.get(file))) {
                // modified file — hashes differ
                String oldContent = ObjectReader.read(files1.get(file));
                String newContent = ObjectReader.read(files2.get(file));
                System.out.println("diff --git a/" + file + " b/" + file);
                System.out.println("--- a/" + file);
                System.out.println("+++ b/" + file);
                printDiff(oldContent, newContent);
            }
            // identical hash = no change, skip
        }
    }

    private static void printDiff(String oldContent, String newContent) {
        List<String> oldLines = splitLines(oldContent);
        List<String> newLines = splitLines(newContent);

        // Simple unified diff — show all lines with context
        int oldSize = oldLines.size();
        int newSize = newLines.size();

        System.out.println("@@ -1," + oldSize + " +1," + newSize + " @@");

        // LCS-based diff
        int[][] lcs = computeLCS(oldLines, newLines);
        printLCS(lcs, oldLines, newLines, oldLines.size(), newLines.size());
    }

    private static int[][] computeLCS(List<String> a, List<String> b) {
        int m = a.size(), n = b.size();
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (a.get(i - 1).equals(b.get(j - 1))) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        return dp;
    }

    private static void printLCS(int[][] dp, List<String> a, List<String> b, int i, int j) {
        if (i == 0 && j == 0) return;

        if (i == 0) {
            printLCS(dp, a, b, i, j - 1);
            System.out.println("+" + b.get(j - 1));
        } else if (j == 0) {
            printLCS(dp, a, b, i - 1, j);
            System.out.println("-" + a.get(i - 1));
        } else if (a.get(i - 1).equals(b.get(j - 1))) {
            printLCS(dp, a, b, i - 1, j - 1);
            System.out.println(" " + a.get(i - 1));
        } else if (dp[i - 1][j] >= dp[i][j - 1]) {
            printLCS(dp, a, b, i - 1, j);
            System.out.println("-" + a.get(i - 1));
        } else {
            printLCS(dp, a, b, i, j - 1);
            System.out.println("+" + b.get(j - 1));
        }
    }

    private static List<String> splitLines(String content) {
        List<String> lines = new ArrayList<>();
        if (content == null || content.isEmpty()) return lines;
        for (String line : content.split("\n", -1)) {
            lines.add(line);
        }
        // remove trailing empty string from split
        if (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }
        return lines;
    }
}