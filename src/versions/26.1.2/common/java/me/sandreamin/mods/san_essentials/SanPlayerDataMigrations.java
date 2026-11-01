package me.sandreamin.mods.san_essentials;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Backs up and upgrades legacy 26.1.2 player data before normal deserialization.
 */
final class SanPlayerDataMigrations {
    static final int CURRENT_SCHEMA_VERSION = 2;
    static final String SCHEMA_VERSION_KEY = "schemaVersion";
    private static final Set<MinecraftServer> CHECKED_SERVERS =
            Collections.newSetFromMap(new WeakHashMap<>());

    private SanPlayerDataMigrations() {
    }

    static synchronized boolean backupIfNeeded(MinecraftServer server, Path dataPath) {
        if (CHECKED_SERVERS.contains(server)) {
            return false;
        }
        if (!Files.isRegularFile(dataPath)) {
            CHECKED_SERVERS.add(server);
            return false;
        }

        try {
            CompoundTag payload = NbtIo.readCompressed(dataPath, NbtAccounter.unlimitedHeap())
                    .getCompound("data")
                    .orElseThrow(() -> new IOException("Missing data compound"));
            if (payload.getInt(SCHEMA_VERSION_KEY).orElse(0) < CURRENT_SCHEMA_VERSION) {
                backup(dataPath);
                CHECKED_SERVERS.add(server);
                return true;
            }
            CHECKED_SERVERS.add(server);
            return false;
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Failed to back up San player data before migration: " + dataPath, exception);
        }
    }

    static boolean migrate(CompoundTag tag) {
        int version = tag.getInt(SCHEMA_VERSION_KEY).orElse(0);
        if (version > CURRENT_SCHEMA_VERSION) {
            throw new IllegalStateException("Unsupported San player data schema version: " + version);
        }

        boolean migrated = false;
        while (version < CURRENT_SCHEMA_VERSION) {
            switch (version) {
                case 0 -> migrateV0ToV1(tag);
                case 1 -> migrateV1ToV2(tag);
                default -> throw new IllegalStateException("Missing San player data migration for schema version: " + version);
            }
            version++;
            migrated = true;
        }
        return migrated;
    }

    private static void backup(Path dataPath) throws IOException {
        Path backupPath = dataPath.resolveSibling(dataPath.getFileName() + ".bak");
        Files.copy(
                dataPath,
                backupPath,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES
        );
        SanEssentials.LOGGER.info("Backed up San Essentials player data from {} to {}", dataPath, backupPath);
    }

    private static void migrateV0ToV1(CompoundTag tag) {
        tag.putInt(SCHEMA_VERSION_KEY, 1);
    }

    private static void migrateV1ToV2(CompoundTag tag) {
        tag.putInt(SCHEMA_VERSION_KEY, 2);
    }
}
