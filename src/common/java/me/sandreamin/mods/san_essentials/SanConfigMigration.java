package me.sandreamin.mods.san_essentials;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;

/**
 * Removes obsolete configuration entries while preserving a backup of the original file.
 */
final class SanConfigMigration {
    private static final String COMMANDS_SECTION = "commands";
    private static final String COMMAND_SECTION_PREFIX = COMMANDS_SECTION + ".";
    private static final Set<String> COMMAND_KEYS = Set.of("enabled", "permissionLevel");
    private static final String BACKUP_SUFFIX = ".pre-config-migration.bak";

    private SanConfigMigration() {
    }

    static boolean removeUnknownOptions(Path path) throws IOException {
        String original = Files.readString(path, StandardCharsets.UTF_8);
        String migrated = removeUnknownOptions(original);
        if (migrated.equals(original)) {
            return false;
        }

        Path backup = backupPath(path);
        if (Files.notExists(backup)) {
            // Keep the first pre-migration snapshot so later reloads cannot overwrite it.
            Files.copy(path, backup);
        }

        Path temporary = Files.createTempFile(path.getParent(), path.getFileName().toString(), ".tmp");
        try {
            Files.writeString(temporary, migrated, StandardCharsets.UTF_8);
            try {
                Files.move(
                        temporary,
                        path,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
        return true;
    }

    static Path backupPath(Path path) {
        return path.resolveSibling(path.getFileName() + BACKUP_SUFFIX);
    }

    private static String removeUnknownOptions(String content) {
        StringBuilder migrated = new StringBuilder(content.length());
        StringBuilder pendingComments = new StringBuilder();
        Section section = Section.ROOT;

        for (String line : content.split("(?<=\\n)", -1)) {
            // Comments are emitted only when the setting or section they describe is retained.
            String syntax = stripComment(line).trim();
            if (syntax.isEmpty()) {
                pendingComments.append(line);
                continue;
            }

            if (syntax.startsWith("[") && syntax.endsWith("]")) {
                String sectionName = syntax.substring(1, syntax.length() - 1).trim();
                section = classifySection(sectionName);
                if (section != Section.UNKNOWN) {
                    migrated.append(pendingComments).append(line);
                }
                pendingComments.setLength(0);
                continue;
            }

            int equals = syntax.indexOf('=');
            if (equals >= 0) {
                String key = syntax.substring(0, equals).trim();
                if (isKnownKey(section, key)) {
                    migrated.append(pendingComments).append(line);
                }
                pendingComments.setLength(0);
                continue;
            }

            if (section != Section.UNKNOWN) {
                migrated.append(pendingComments).append(line);
            }
            pendingComments.setLength(0);
        }

        if (section != Section.UNKNOWN) {
            migrated.append(pendingComments);
        }
        return migrated.toString();
    }

    private static Section classifySection(String sectionName) {
        if (COMMANDS_SECTION.equals(sectionName)) {
            return Section.COMMANDS;
        }
        if (sectionName.startsWith(COMMAND_SECTION_PREFIX)
                && SanConfig.isCommandName(sectionName.substring(COMMAND_SECTION_PREFIX.length()))) {
            return Section.COMMAND;
        }
        return Section.UNKNOWN;
    }

    private static boolean isKnownKey(Section section, String key) {
        return switch (section) {
            case ROOT -> SanConfig.isValueKey(key);
            case COMMAND -> COMMAND_KEYS.contains(key);
            case COMMANDS, UNKNOWN -> false;
        };
    }

    private static String stripComment(String line) {
        int hash = line.indexOf('#');
        return hash >= 0 ? line.substring(0, hash) : line;
    }

    private enum Section {
        ROOT,
        COMMANDS,
        COMMAND,
        UNKNOWN
    }
}
