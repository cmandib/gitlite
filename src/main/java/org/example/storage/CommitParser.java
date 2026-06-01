package org.example.storage;

public class CommitParser {

    public record CommitObject(
            String tree,
            String parent,
            String parent2,
            String author,
            String message
    ) {}

    public static CommitObject parse(String raw) {
        String tree = null;
        String parent = null;
        String parent2 = null;
        String author = null;
        StringBuilder message = new StringBuilder();

        String[] lines = raw.split("\n", -1);
        boolean inMessage = false;

        for (String line : lines) {
            if (inMessage) {
                message.append(line).append("\n");
                continue;
            }

            if (line.isEmpty()) {
                inMessage = true;
                continue;
            }

            if (line.startsWith("tree "))        tree    = line.substring(5).trim();
            else if (line.startsWith("parent ")) {
                if (parent == null)              parent  = line.substring(7).trim();
                else                             parent2 = line.substring(7).trim();
            }
            else if (line.startsWith("author ")) author  = line.substring(7).trim();
        }

        return new CommitObject(tree, parent, parent2, author, message.toString().trim());
    }
}