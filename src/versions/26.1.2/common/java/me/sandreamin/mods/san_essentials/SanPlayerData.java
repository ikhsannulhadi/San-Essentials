package me.sandreamin.mods.san_essentials;

import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Adapts the shared data store to Minecraft 26.1.2 SavedData serialization.
 */
public class SanPlayerData extends SanPlayerDataStore {
    private static final String NAME = "player_data";
    private static final Codec<SanPlayerData> CODEC = CompoundTag.CODEC.xmap(SanPlayerData::load, SanPlayerData::saveTag);
    private static final SavedDataType<SanPlayerData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SanEssentials.MODID, NAME),
            SanPlayerData::new,
            CODEC,
            null
    );

    public static SanPlayerData get(MinecraftServer server) {
        Path currentDataPath = server.getWorldPath(LevelResource.DATA)
                .resolve(SanEssentials.MODID)
                .resolve("player_data.dat");
        boolean migrationNeeded = SanPlayerDataMigrations.backupIfNeeded(server, currentDataPath);
        SanPlayerData data = server.getDataStorage().computeIfAbsent(TYPE);
        if (migrationNeeded) {
            data.setDirty();
            server.getDataStorage().saveAndJoin();
            SanEssentials.LOGGER.info(
                    "Migrated San Essentials player data to schema version {}",
                    SanPlayerDataMigrations.CURRENT_SCHEMA_VERSION
            );
        }
        return data;
    }

    static SanPlayerData load(CompoundTag tag) {
        boolean migrated = SanPlayerDataMigrations.migrate(tag);
        SanPlayerData data = new SanPlayerData();
        data.readLocationMap(list(tag, "homes"), data.homes);
        data.readLocationMap(list(tag, "deathBacks"), data.deathBacks);
        data.readLocationMap(list(tag, "teleportBacks"), data.teleportBacks);
        data.readWarpMap(list(tag, "warps"), data.warps);

        data.readUuidSet(list(tag, "tpaAuto"), data.tpaAuto);
        data.readUuidSet(list(tag, "fly"), data.fly);
        data.readUuidSet(list(tag, "god"), data.god);
        data.readUuidSet(list(tag, "backNotice"), data.backNotice);
        data.readUuidSet(list(tag, "firstJoinNotice"), data.firstJoinNotice);
        if (migrated) {
            data.setDirty();
        }
        return data;
    }

    private CompoundTag saveTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(SanPlayerDataMigrations.SCHEMA_VERSION_KEY, SanPlayerDataMigrations.CURRENT_SCHEMA_VERSION);
        tag.put("homes", writeLocationMap(homes));
        tag.put("deathBacks", writeLocationMap(deathBacks));
        tag.put("teleportBacks", writeLocationMap(teleportBacks));
        tag.put("warps", writeWarpMap(warps));

        tag.put("tpaAuto", writeUuidSet(tpaAuto));
        tag.put("fly", writeUuidSet(fly));
        tag.put("god", writeUuidSet(god));
        tag.put("backNotice", writeUuidSet(backNotice));
        tag.put("firstJoinNotice", writeUuidSet(firstJoinNotice));
        return tag;
    }

    private void readLocationMap(ListTag list, Map<UUID, SanLocation> target) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i).orElseGet(CompoundTag::new);
            target.put(readUuid(entry), SanLocation.load(entry.getCompound("location").orElseGet(CompoundTag::new)));
        }
    }

    private ListTag writeLocationMap(Map<UUID, SanLocation> source) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, SanLocation> item : source.entrySet()) {
            CompoundTag entry = new CompoundTag();
            writeUuid(entry, item.getKey());
            entry.put("location", item.getValue().save());
            list.add(entry);
        }
        return list;
    }

    private void readWarpMap(ListTag list, Map<String, SanLocation> target) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i).orElseGet(CompoundTag::new);
            target.put(entry.getString("name").orElse(""), SanLocation.load(entry.getCompound("location").orElseGet(CompoundTag::new)));
        }
    }

    private ListTag writeWarpMap(Map<String, SanLocation> source) {
        ListTag list = new ListTag();
        for (Map.Entry<String, SanLocation> item : source.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("name", item.getKey());
            entry.put("location", item.getValue().save());
            list.add(entry);
        }
        return list;
    }

    private void readUuidSet(ListTag list, Set<UUID> target) {
        for (int i = 0; i < list.size(); i++) {
            target.add(readUuid(list.getCompound(i).orElseGet(CompoundTag::new)));
        }
    }

    private ListTag writeUuidSet(Set<UUID> source) {
        ListTag list = new ListTag();
        for (UUID uuid : source) {
            CompoundTag entry = new CompoundTag();
            writeUuid(entry, uuid);
            list.add(entry);
        }
        return list;
    }

    private static ListTag list(CompoundTag tag, String name) {
        return tag.getList(name).orElseGet(ListTag::new);
    }

    private static UUID readUuid(CompoundTag tag) {
        return tag.getIntArray("uuid")
                .filter(value -> value.length == 4)
                .map(UUIDUtil::uuidFromIntArray)
                .or(() -> tag.getString("uuid").map(UUID::fromString))
                .orElse(new UUID(0L, 0L));
    }

    private static void writeUuid(CompoundTag tag, UUID uuid) {
        tag.putIntArray("uuid", UUIDUtil.uuidToIntArray(uuid));
    }
}
