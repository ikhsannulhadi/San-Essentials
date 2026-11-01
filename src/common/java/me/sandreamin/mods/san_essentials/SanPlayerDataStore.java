package me.sandreamin.mods.san_essentials;

import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Holds persistent player and server state independently of version-specific NBT APIs.
 */
public abstract class SanPlayerDataStore extends SavedData {
    protected final Map<UUID, SanLocation> homes = new HashMap<>();
    protected final Map<UUID, SanLocation> deathBacks = new HashMap<>();
    protected final Map<UUID, SanLocation> teleportBacks = new HashMap<>();
    protected final Map<String, SanLocation> warps = new HashMap<>();
    protected final Set<UUID> tpaAuto = new HashSet<>();
    protected final Set<UUID> fly = new HashSet<>();
    protected final Set<UUID> god = new HashSet<>();
    protected final Set<UUID> backNotice = new HashSet<>();
    protected final Set<UUID> firstJoinNotice = new HashSet<>();

    public Optional<SanLocation> home(UUID player) {
        return Optional.ofNullable(homes.get(player));
    }

    public void setHome(UUID player, SanLocation location) {
        homes.put(player, location);
        setDirty();
    }

    public boolean deleteHome(UUID player) {
        boolean removed = homes.remove(player) != null;
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public Optional<SanLocation> deathBack(UUID player) {
        return Optional.ofNullable(deathBacks.get(player));
    }

    public void setDeathBack(UUID player, SanLocation location) {
        deathBacks.put(player, location);
        setDirty();
    }

    public Optional<SanLocation> teleportBack(UUID player) {
        return Optional.ofNullable(teleportBacks.get(player));
    }

    public void setTeleportBack(UUID player, SanLocation location) {
        teleportBacks.put(player, location);
        setDirty();
    }

    public boolean isTpaAuto(UUID player) {
        return tpaAuto.contains(player);
    }

    public boolean toggleTpaAuto(UUID player) {
        return toggleUuidSet(tpaAuto, player);
    }

    public boolean isFlyEnabled(UUID player) {
        return fly.contains(player);
    }

    public boolean toggleFly(UUID player) {
        return toggleUuidSet(fly, player);
    }

    public boolean isGodEnabled(UUID player) {
        return god.contains(player);
    }

    public boolean toggleGod(UUID player) {
        return toggleUuidSet(god, player);
    }

    public boolean markBackNoticeShown(UUID player) {
        boolean added = backNotice.add(player);
        if (added) {
            setDirty();
        }
        return added;
    }

    public boolean hasFirstJoinNoticeShown(UUID player) {
        return firstJoinNotice.contains(player);
    }

    public boolean markFirstJoinNoticeShown(UUID player) {
        boolean added = firstJoinNotice.add(player);
        if (added) {
            setDirty();
        }
        return added;
    }

    public Optional<SanLocation> warp(String name) {
        return Optional.ofNullable(warps.get(name));
    }

    public Set<String> warpNames() {
        return Set.copyOf(warps.keySet());
    }

    public void setWarp(String name, SanLocation location) {
        warps.put(name, location);
        setDirty();
    }

    public boolean deleteWarp(String name) {
        boolean removed = warps.remove(name) != null;
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public void clearPlayerData() {
        homes.clear();
        deathBacks.clear();
        teleportBacks.clear();
        tpaAuto.clear();
        fly.clear();
        god.clear();
        clearNoticeData();
        setDirty();
    }

    public void clearNoticeData() {
        backNotice.clear();
        firstJoinNotice.clear();
        setDirty();
    }

    private boolean toggleUuidSet(Set<UUID> target, UUID uuid) {
        // Every state transition marks the SavedData instance for persistence.
        boolean enabled;
        if (target.contains(uuid)) {
            target.remove(uuid);
            enabled = false;
        } else {
            target.add(uuid);
            enabled = true;
        }
        setDirty();
        return enabled;
    }
}
