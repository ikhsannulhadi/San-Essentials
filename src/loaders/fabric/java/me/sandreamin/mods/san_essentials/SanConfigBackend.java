package me.sandreamin.mods.san_essentials;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Provides Fabric's lightweight TOML persistence and live-reload support.
 */
final class SanConfigBackend {
    private static final long RELOAD_CHECK_INTERVAL_NANOS = 1_000_000_000L;
    private static final Map<String, SanConfig.ValueDefinition> DEFINITIONS = SanConfig.values().stream()
            .collect(Collectors.toUnmodifiableMap(SanConfig.ValueDefinition::key, Function.identity()));
    private static final Map<String, Object> VALUES = new LinkedHashMap<>();
    private static final Map<String, CommandValue> COMMANDS = new LinkedHashMap<>();

    private static long lastLoadedModifiedMillis = Long.MIN_VALUE;
    private static long lastReloadCheckNanos = Long.MIN_VALUE;

    static {
        resetDefaults();
    }

    private SanConfigBackend() {
    }

    static void load() {
        Path path = configPath();
        if (!Files.exists(path)) {
            writeDefault(path);
            updateLastLoaded(path);
            return;
        }

        migrateLegacyConfig(path);
        try {
            resetDefaults();
            parse(Files.readAllLines(path, StandardCharsets.UTF_8));
            updateLastLoaded(path);
        } catch (IOException exception) {
            SanEssentials.LOGGER.warn("Failed to load San Essentials config: {}", path, exception);
        }
    }

    private static void migrateLegacyConfig(Path path) {
        try {
            if (SanConfigMigration.removeUnknownOptions(path)) {
                SanEssentials.LOGGER.info(
                        "Removed unknown settings from San Essentials config; backup: {}",
                        SanConfigMigration.backupPath(path)
                );
            }
        } catch (IOException exception) {
            SanEssentials.LOGGER.warn("Failed to migrate San Essentials config: {}", path, exception);
        }
    }

    static String stringValue(SanConfig.ValueDefinition definition) {
        reloadIfChanged();
        return (String) VALUES.get(definition.key());
    }

    static boolean booleanValue(SanConfig.ValueDefinition definition) {
        reloadIfChanged();
        return (boolean) VALUES.get(definition.key());
    }

    static int intValue(SanConfig.ValueDefinition definition) {
        reloadIfChanged();
        return (int) VALUES.get(definition.key());
    }

    static boolean commandEnabled(String command) {
        reloadIfChanged();
        CommandValue value = COMMANDS.get(command);
        return value == null || value.enabled;
    }

    static int commandPermission(String command) {
        reloadIfChanged();
        CommandValue value = COMMANDS.get(command);
        return value == null ? 0 : value.permissionLevel;
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(SanConfig.FILE_NAME);
    }

    private static void reloadIfChanged() {
        long currentNanos = System.nanoTime();
        // Avoid a filesystem timestamp lookup for every command-side configuration read.
        if (lastReloadCheckNanos != Long.MIN_VALUE
                && currentNanos - lastReloadCheckNanos < RELOAD_CHECK_INTERVAL_NANOS) {
            return;
        }
        lastReloadCheckNanos = currentNanos;

        Path path = configPath();
        if (!Files.exists(path)) {
            return;
        }

        try {
            long modifiedMillis = Files.getLastModifiedTime(path).toMillis();
            if (modifiedMillis != lastLoadedModifiedMillis) {
                load();
            }
        } catch (IOException exception) {
            SanEssentials.LOGGER.warn("Failed to check San Essentials config timestamp: {}", path, exception);
        }
    }

    private static void updateLastLoaded(Path path) {
        try {
            lastLoadedModifiedMillis = Files.exists(path) ? Files.getLastModifiedTime(path).toMillis() : Long.MIN_VALUE;
        } catch (IOException exception) {
            lastLoadedModifiedMillis = Long.MIN_VALUE;
        }
    }

    private static void resetDefaults() {
        VALUES.clear();
        for (SanConfig.ValueDefinition definition : SanConfig.values()) {
            VALUES.put(definition.key(), definition.defaultValue());
        }

        COMMANDS.clear();
        for (SanConfig.CommandDefinition command : SanConfig.commands()) {
            COMMANDS.put(command.name(), new CommandValue(command.enabled(), command.permissionLevel()));
        }
    }

    private static void parse(Iterable<String> lines) {
        String section = "";
        for (String rawLine : lines) {
            String line = stripComment(rawLine).trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1).trim();
                continue;
            }

            int equals = line.indexOf('=');
            if (equals < 0) {
                continue;
            }

            String key = line.substring(0, equals).trim();
            String value = line.substring(equals + 1).trim();
            if (section.startsWith("commands.")) {
                parseCommand(section.substring("commands.".length()), key, value);
            } else {
                parseRoot(key, value);
            }
        }
    }

    private static void parseRoot(String key, String value) {
        SanConfig.ValueDefinition definition = DEFINITIONS.get(key);
        if (definition == null) {
            return;
        }

        Object parsedValue = switch (definition.type()) {
            case STRING -> parseString(value);
            case BOOLEAN -> parseBoolean(value);
            case INTEGER -> parseInt(value, definition.min(), definition.max());
        };
        if (parsedValue != null && definition.accepts(parsedValue)) {
            VALUES.put(key, parsedValue);
        }
    }

    private static void parseCommand(String command, String key, String value) {
        CommandValue commandValue = COMMANDS.get(command);
        if (commandValue == null) {
            return;
        }

        if ("enabled".equals(key)) {
            Boolean parsedValue = parseBoolean(value);
            if (parsedValue != null) {
                commandValue.enabled = parsedValue;
            }
        } else if ("permissionLevel".equals(key)) {
            Integer parsedValue = parseInt(value, 0, 4);
            if (parsedValue != null) {
                commandValue.permissionLevel = parsedValue;
            }
        }
    }

    private static String stripComment(String line) {
        int hash = line.indexOf('#');
        return hash >= 0 ? line.substring(0, hash) : line;
    }

    private static String parseString(String value) {
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static Boolean parseBoolean(String value) {
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "true" -> true;
            case "false" -> false;
            default -> null;
        };
    }

    private static Integer parseInt(String value, int min, int max) {
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(value.trim())));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static void writeDefault(Path path) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, defaultConfig(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            SanEssentials.LOGGER.warn("Failed to write default San Essentials config: {}", path, exception);
        }
    }

    private static String defaultConfig() {
        StringBuilder builder = new StringBuilder();
        for (SanConfig.ValueDefinition definition : SanConfig.values()) {
            appendComments(builder, definition.commentsWithDefaultValue());
            builder.append(definition.key())
                    .append(" = ")
                    .append(formatValue(definition.defaultValue()))
                    .append('\n');
        }

        builder.append('\n');
        appendComments(builder, SanConfig.commandComments());
        builder.append("[commands]\n\n");
        for (SanConfig.CommandDefinition command : SanConfig.commands()) {
            builder.append("[commands.").append(command.name()).append("]\n");
            appendComments(builder, command.enabledComments());
            builder.append("enabled = ").append(command.enabled()).append('\n');
            appendComments(builder, command.permissionLevelComments());
            builder.append("permissionLevel = ").append(command.permissionLevel()).append("\n\n");
        }
        return builder.toString();
    }

    private static void appendComments(StringBuilder builder, Iterable<String> comments) {
        for (String comment : comments) {
            builder.append('#').append(comment).append('\n');
        }
    }

    private static String formatValue(Object value) {
        return value instanceof String stringValue ? "\"" + stringValue + "\"" : value.toString();
    }

    private static final class CommandValue {
        private boolean enabled;
        private int permissionLevel;

        private CommandValue(boolean enabled, int permissionLevel) {
            this.enabled = enabled;
            this.permissionLevel = permissionLevel;
        }
    }
}
