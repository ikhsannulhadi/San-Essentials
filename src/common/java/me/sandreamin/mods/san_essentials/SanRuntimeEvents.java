package me.sandreamin.mods.san_essentials;

import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Implements lifecycle and player event behavior shared by Fabric and NeoForge.
 */
public final class SanRuntimeEvents {
    private static final Map<UUID, Integer> BACK_NOTICE_TICKS = new HashMap<>();
    private static final Map<UUID, Integer> FIRST_JOIN_NOTICE_TICKS = new HashMap<>();

    private SanRuntimeEvents() {
    }

    public static void onPlayerDeath(ServerPlayer player) {
        SanPlayerData.get(SanCompat.server(player)).setDeathBack(player.getUUID(), SanLocation.of(player));
    }

    public static boolean allowsPlayerDamage(ServerPlayer player) {
        return !SanPlayerData.get(SanCompat.server(player)).isGodEnabled(player.getUUID());
    }

    public static void onPlayerRespawn(ServerPlayer player) {
        SanCommands.restoreFly(player);
        if (SanPlayerData.get(SanCompat.server(player)).markBackNoticeShown(player.getUUID())) {
            // Delay the message until the respawned player is ready to receive UI updates.
            BACK_NOTICE_TICKS.put(player.getUUID(), SanCompat.server(player).getTickCount() + 10);
        }
    }

    public static void onPlayerLogin(ServerPlayer player) {
        MinecraftServer server = SanCompat.server(player);
        SanCommands.restoreFly(player);
        if (SanConfig.firstJoinNotice() && !SanPlayerData.get(server).hasFirstJoinNoticeShown(player.getUUID())) {
            // A short delay keeps the notice from competing with initial join packets.
            FIRST_JOIN_NOTICE_TICKS.put(player.getUUID(), server.getTickCount() + 40);
        }
    }

    public static void onServerStarted(MinecraftServer server) {
        SanPlayerData.get(server);
    }

    public static void onServerStopped() {
        SanCommands.clearRuntimeState();
        clearNoticeQueues();
    }

    public static void clearNoticeQueues() {
        BACK_NOTICE_TICKS.clear();
        FIRST_JOIN_NOTICE_TICKS.clear();
    }

    public static void onServerTick(MinecraftServer server) {
        SanCommands.onServerTick(server);

        Iterator<Map.Entry<UUID, Integer>> backIterator = BACK_NOTICE_TICKS.entrySet().iterator();
        while (backIterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = backIterator.next();
            if (entry.getValue() > server.getTickCount()) {
                continue;
            }

            backIterator.remove();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                SanCompat.playSound(player, SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 1.0F, 1.2F);
                player.sendSystemMessage(SanLang.prefixedComponent(player, "san_essentials.back.notice")
                        .withStyle(Style.EMPTY
                                .withClickEvent(SanCompat.suggestCommandClick("/back"))
                                .withHoverEvent(SanCompat.showTextHover(SanLang.component(player, "san_essentials.back.notice.tooltip")))));
            }
        }

        Iterator<Map.Entry<UUID, Integer>> joinIterator = FIRST_JOIN_NOTICE_TICKS.entrySet().iterator();
        while (joinIterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = joinIterator.next();
            if (entry.getValue() > server.getTickCount()) {
                continue;
            }

            joinIterator.remove();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null && SanPlayerData.get(server).markFirstJoinNoticeShown(player.getUUID())) {
                SanCompat.playSound(player, SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 1.0F, 1.2F);
                player.sendSystemMessage(SanLang.prefixedComponent(player, "san_essentials.first_join.notice")
                        .withStyle(Style.EMPTY
                                .withClickEvent(SanCompat.runCommandClick("/san-ess help"))
                                .withHoverEvent(SanCompat.showTextHover(SanLang.component(player, "san_essentials.first_join.notice.tooltip")))));
            }
        }
    }
}
