package me.sandreamin.mods.san_essentials;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Connects NeoForge events to shared behavior and loader-specific protections.
 */
public final class SanEvents {
    private SanEvents() {
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SanRuntimeEvents.onPlayerDeath(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !SanRuntimeEvents.allowsPlayerDamage(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SanRuntimeEvents.onPlayerRespawn(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SanRuntimeEvents.onPlayerLogin(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SanRuntimeEvents.onPlayerLogout(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTeleport(EntityTeleportEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SanPlayerData.get(SanCompat.server(player)).setTeleportBack(
                    player.getUUID(),
                    new SanLocation(SanCompat.level(player).dimension(), event.getPrevX(), event.getPrevY(), event.getPrevZ(), player.getYRot(), player.getXRot())
            );
        }
    }

    @SubscribeEvent
    public static void onMobGriefing(EntityMobGriefingEvent event) {
        if (SanMobGriefing.shouldPreventBlockGriefing(event.getEntity())) {
            event.setCanGrief(false);
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (SanMobGriefing.shouldPreventCreeperExplosionBlocks(event.getExplosion())) {
            event.getAffectedBlocks().clear();
        }
    }

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        if (SanConfig.commandEnabled("repeat")
                && event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer player
                && SanCompat.hasPermission(player, SanConfig.commandPermission("repeat"))) {
            SanCommands.rememberCommand(player, event.getParseResults().getReader().getString());
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        SanRuntimeEvents.onServerStarted(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SanRuntimeEvents.onServerStopped();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        SanRuntimeEvents.onServerTick(event.getServer());
    }
}
