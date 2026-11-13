package me.sandreamin.mods.san_essentials;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Adapts the shared data store to Minecraft 1.21.1 SavedData serialization.
 */
public class SanPlayerData extends SanPlayerDataStore {
    private static final String NAME = SanEssentials.MODID + "_player_data";

    public static SanPlayerData get(MinecraftServer server) {
        boolean migrationNeeded = SanPlayerDataMigrations.backupIfNeeded(server);
        var storage = server.getLevel(Level.OVERWORLD).getDataStorage();
        SanPlayerData data = storage.computeIfAbsent(
                new SavedData.Factory<>(SanPlayerData::new, SanPlayerData::load, null),
                NAME
        );
        if (migrationNeeded) {
            data.setDirty();
            storage.save();
            SanPlayerDataMigrations.awaitSaved(server);
            SanEssentials.LOGGER.info(
                    "Migrated San Essentials player data to schema version {}",
                    SanPlayerDataMigrations.CURRENT_SCHEMA_VERSION
            );
        }
        return data;
    }

    private static SanPlayerData load(CompoundTag tag, HolderLookup.Provider provider) {
        boolean migrated = SanPlayerDataMigrations.migrate(tag);
        SanPlayerData data = new SanPlayerData();
        data.readHomeMap(tag.getList("homes", Tag.TAG_COMPOUND), data.homes);
        data.readLocationMap(tag.getList("deathBacks", Tag.TAG_COMPOUND), data.deathBacks);
        data.readLocationMap(tag.getList("teleportBacks", Tag.TAG_COMPOUND), data.teleportBacks);
        data.readNamedLocationMap(tag.getList("warps", Tag.TAG_COMPOUND), data.warps);
        data.readIgnoreMap(tag.getList("tpIgnores", Tag.TAG_COMPOUND), data.tpIgnores);

        data.readUuidSet(tag.getList("tpaAuto", Tag.TAG_COMPOUND), data.tpaAuto);
        data.readUuidSet(tag.getList("fly", Tag.TAG_COMPOUND), data.fly);
        data.readUuidSet(tag.getList("god", Tag.TAG_COMPOUND), data.god);
        data.readUuidSet(tag.getList("backNotice", Tag.TAG_COMPOUND), data.backNotice);
        data.readUuidSet(tag.getList("firstJoinNotice", Tag.TAG_COMPOUND), data.firstJoinNotice);
        if (migrated) {
            data.setDirty();
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt(SanPlayerDataMigrations.SCHEMA_VERSION_KEY, SanPlayerDataMigrations.CURRENT_SCHEMA_VERSION);
        tag.put("homes", writeHomeMap(homes));
        tag.put("deathBacks", writeLocationMap(deathBacks));
        tag.put("teleportBacks", writeLocationMap(teleportBacks));
        tag.put("warps", writeNamedLocationMap(warps));
        tag.put("tpIgnores", writeIgnoreMap(tpIgnores));

        tag.put("tpaAuto", writeUuidSet(tpaAuto));
        tag.put("fly", writeUuidSet(fly));
        tag.put("god", writeUuidSet(god));
        tag.put("backNotice", writeUuidSet(backNotice));
        tag.put("firstJoinNotice", writeUuidSet(firstJoinNotice));
        return tag;
    }

    private void readLocationMap(ListTag list, Map<UUID, SanLocation> target) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            UUID uuid = entry.getUUID("uuid");
            if (uuid != null) {
                target.put(uuid, SanLocation.load(entry.getCompound("location")));
            }
        }
    }

    private ListTag writeLocationMap(Map<UUID, SanLocation> source) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, SanLocation> item : source.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("uuid", item.getKey());
            entry.put("location", item.getValue().save());
            list.add(entry);
        }
        return list;
    }

    private void readHomeMap(ListTag list, Map<UUID, Map<String, SanLocation>> target) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            UUID uuid = entry.getUUID("uuid");
            if (uuid == null) {
                continue;
            }

            Map<String, SanLocation> owned = new HashMap<>();
            readNamedLocationMap(entry.getList("homes", Tag.TAG_COMPOUND), owned);
            if (!owned.isEmpty()) {
                target.put(uuid, owned);
            }
        }
    }

    private ListTag writeHomeMap(Map<UUID, Map<String, SanLocation>> source) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Map<String, SanLocation>> item : source.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("uuid", item.getKey());
            entry.put("homes", writeNamedLocationMap(item.getValue()));
            list.add(entry);
        }
        return list;
    }

    private void readNamedLocationMap(ListTag list, Map<String, SanLocation> target) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            String name = entry.getString("name");
            if (!name.isEmpty()) {
                target.put(name, SanLocation.load(entry.getCompound("location")));
            }
        }
    }

    private ListTag writeNamedLocationMap(Map<String, SanLocation> source) {
        ListTag list = new ListTag();
        for (Map.Entry<String, SanLocation> item : source.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("name", item.getKey());
            entry.put("location", item.getValue().save());
            list.add(entry);
        }
        return list;
    }

    private void readIgnoreMap(ListTag list, Map<UUID, Map<UUID, String>> target) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            UUID owner = entry.getUUID("uuid");
            UUID ignored = entry.getUUID("target");
            if (owner == null || ignored == null) {
                continue;
            }

            String name = entry.getString("name");
            if (name.isEmpty()) {
                name = ignored.toString();
            }
            target.computeIfAbsent(owner, key -> new HashMap<>()).put(ignored, name);
        }
    }

    private ListTag writeIgnoreMap(Map<UUID, Map<UUID, String>> source) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Map<UUID, String>> owner : source.entrySet()) {
            for (Map.Entry<UUID, String> ignored : owner.getValue().entrySet()) {
                CompoundTag entry = new CompoundTag();
                entry.putUUID("uuid", owner.getKey());
                entry.putUUID("target", ignored.getKey());
                entry.putString("name", ignored.getValue());
                list.add(entry);
            }
        }
        return list;
    }

    private void readUuidSet(ListTag list, Set<UUID> target) {
        for (int i = 0; i < list.size(); i++) {
            UUID uuid = list.getCompound(i).getUUID("uuid");
            if (uuid != null) {
                target.add(uuid);
            }
        }
    }

    private ListTag writeUuidSet(Set<UUID> source) {
        ListTag list = new ListTag();
        for (UUID uuid : source) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("uuid", uuid);
            list.add(entry);
        }
        return list;
    }
}
