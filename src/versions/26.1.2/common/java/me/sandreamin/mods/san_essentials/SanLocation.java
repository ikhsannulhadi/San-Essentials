package me.sandreamin.mods.san_essentials;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Serializable teleport destination containing a dimension, position, and orientation.
 */
public record SanLocation(ResourceKey<Level> level, double x, double y, double z, float yRot, float xRot) {
    public static SanLocation of(ServerPlayer player) {
        return new SanLocation(SanCompat.level(player).dimension(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("level", level.identifier().toString());
        tag.putDouble("x", x);
        tag.putDouble("y", y);
        tag.putDouble("z", z);
        tag.putFloat("yRot", yRot);
        tag.putFloat("xRot", xRot);
        return tag;
    }

    public static SanLocation load(CompoundTag tag) {
        String levelId = tag.getString("level").orElse("minecraft:overworld");
        ResourceKey<Level> level = ResourceKey.create(Registries.DIMENSION, Identifier.parse(levelId));
        return new SanLocation(
                level,
                tag.getDouble("x").orElse(0.0D),
                tag.getDouble("y").orElse(0.0D),
                tag.getDouble("z").orElse(0.0D),
                tag.getFloat("yRot").orElse(0.0F),
                tag.getFloat("xRot").orElse(0.0F)
        );
    }

    public Optional<ServerLevel> resolve(MinecraftServer server) {
        return Optional.ofNullable(server.getLevel(level));
    }
}
