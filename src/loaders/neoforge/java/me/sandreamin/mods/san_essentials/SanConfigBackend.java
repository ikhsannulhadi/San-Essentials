package me.sandreamin.mods.san_essentials;

import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

final class SanConfigBackend {
    private static final Map<String, ModConfigSpec.ConfigValue<?>> VALUES = new LinkedHashMap<>();
    private static final Map<String, CommandValue> COMMANDS = new LinkedHashMap<>();

    static final ModConfigSpec SPEC;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        for (SanConfig.ValueDefinition definition : SanConfig.values()) {
            String[] comments = (definition.type() == SanConfig.ValueType.INTEGER
                    ? definition.comments()
                    : definition.commentsWithDefaultValue()).toArray(String[]::new);
            builder.comment(comments);
            VALUES.put(definition.key(), define(builder, definition));
        }

        builder.comment(SanConfig.commandComments().toArray(String[]::new)).push("commands");
        for (SanConfig.CommandDefinition command : SanConfig.commands()) {
            builder.push(command.name());
            COMMANDS.put(command.name(), new CommandValue(
                    builder.comment(command.enabledComments().toArray(String[]::new))
                            .define("enabled", command.enabled()),
                    builder.defineInRange("permissionLevel", command.permissionLevel(), 0, 4)
            ));
            builder.pop();
        }
        builder.pop();
        SPEC = builder.build();
    }

    private SanConfigBackend() {
    }

    static void load() {
        Path path = FMLPaths.CONFIGDIR.get().resolve(SanConfig.FILE_NAME);
        if (!Files.exists(path)) {
            return;
        }

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
        return (String) VALUES.get(definition.key()).get();
    }

    static boolean booleanValue(SanConfig.ValueDefinition definition) {
        return (boolean) VALUES.get(definition.key()).get();
    }

    static int intValue(SanConfig.ValueDefinition definition) {
        return (int) VALUES.get(definition.key()).get();
    }

    static boolean commandEnabled(String command) {
        CommandValue value = COMMANDS.get(command);
        return value == null || value.enabled().get();
    }

    static int commandPermission(String command) {
        CommandValue value = COMMANDS.get(command);
        return value == null ? 0 : value.permissionLevel().get();
    }

    private static ModConfigSpec.ConfigValue<?> define(
            ModConfigSpec.Builder builder,
            SanConfig.ValueDefinition definition
    ) {
        return switch (definition.type()) {
            case STRING -> builder.define(
                    definition.key(),
                    definition.defaultValue(),
                    definition::accepts
            );
            case BOOLEAN -> builder.define(
                    definition.key(),
                    (boolean) definition.defaultValue()
            );
            case INTEGER -> builder.defineInRange(
                    definition.key(),
                    (int) definition.defaultValue(),
                    definition.min(),
                    definition.max()
            );
        };
    }

    private record CommandValue(
            ModConfigSpec.BooleanValue enabled,
            ModConfigSpec.IntValue permissionLevel
    ) {
    }
}
