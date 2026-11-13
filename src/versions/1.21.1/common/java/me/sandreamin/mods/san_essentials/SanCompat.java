package me.sandreamin.mods.san_essentials;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;

/**
 * Isolates Minecraft 1.21.1 API calls from the shared command and event code.
 */
public final class SanCompat {
    private SanCompat() {
    }

    public static MinecraftServer server(ServerPlayer player) {
        return player.server;
    }

    public static ServerLevel level(ServerPlayer player) {
        return player.serverLevel();
    }

    public static ServerLevel spawnLevel(ServerPlayer player) {
        ServerLevel overworld = server(player).getLevel(Level.OVERWORLD);
        return overworld != null ? overworld : level(player);
    }

    public static BlockPos spawnPos(ServerPlayer player) {
        return spawnLevel(player).getSharedSpawnPos();
    }

    public static float spawnAngle(ServerPlayer player) {
        return spawnLevel(player).getSharedSpawnAngle();
    }

    public static int minBuildY(ServerLevel level) {
        return level.getMinBuildHeight();
    }

    public static int maxBuildY(ServerLevel level) {
        return level.getMaxBuildHeight();
    }

    public static String profileName(ServerPlayer player) {
        return player.getGameProfile().getName();
    }

    public static void playSound(ServerPlayer player, SoundEvent sound, SoundSource source, float volume, float pitch) {
        player.playNotifySound(sound, source, volume, pitch);
    }

    public static void kill(ServerPlayer player) {
        player.kill();
    }

    public static void teleport(ServerPlayer player, ServerLevel level, double x, double y, double z, float yRot, float xRot) {
        player.teleportTo(level, x, y, z, yRot, xRot);
    }

    public static boolean hasPermission(CommandSourceStack source, int level) {
        return source.hasPermission(level);
    }

    public static boolean hasPermission(ServerPlayer player, int level) {
        return player.hasPermissions(level);
    }

    @SuppressWarnings("deprecation")
    public static void setMayFly(ServerPlayer player, boolean mayFly) {
        player.getAbilities().mayfly = mayFly;
        if (!mayFly) {
            player.getAbilities().flying = false;
        }
        player.onUpdateAbilities();
    }

    @SuppressWarnings("deprecation")
    public static boolean isFlying(ServerPlayer player) {
        return player.getAbilities().flying;
    }

    @SuppressWarnings("deprecation")
    public static boolean hasFlightAbility(ServerPlayer player) {
        return player.getAbilities().mayfly;
    }

    @SuppressWarnings("deprecation")
    public static void restoreFlight(ServerPlayer player, boolean wasFlying) {
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = wasFlying || player.getAbilities().flying;
        player.onUpdateAbilities();
    }

    public static ClickEvent runCommandClick(String command) {
        return new ClickEvent(ClickEvent.Action.RUN_COMMAND, command);
    }

    public static ClickEvent suggestCommandClick(String command) {
        return new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command);
    }

    public static HoverEvent showTextHover(Component text) {
        return new HoverEvent(HoverEvent.Action.SHOW_TEXT, text);
    }

    public static void openTeleportMenu(ServerPlayer player, boolean here, int page) {
        SanTeleportMenu.open(player, here, page);
    }

    public static void openWarpMenu(ServerPlayer player, int page) {
        SanWarpMenu.open(player, page);
    }

    public static void openHomeMenu(ServerPlayer player, int page) {
        SanHomeMenu.open(player, page);
    }

    public static void setPlayerHeadProfile(ItemStack head, ServerPlayer player) {
        head.set(DataComponents.PROFILE, new ResolvableProfile(player.getGameProfile()));
    }
}
