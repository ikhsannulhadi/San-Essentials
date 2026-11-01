package me.sandreamin.mods.san_essentials;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

/**
 * Connects Fabric lifecycle hooks to the shared runtime event handlers.
 */
public final class SanEvents {
    private SanEvents() {
    }

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayer player) {
                SanRuntimeEvents.onPlayerDeath(player);
            }
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, damageSource, amount) ->
                !(entity instanceof ServerPlayer player) || SanRuntimeEvents.allowsPlayerDamage(player)
        );
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> SanRuntimeEvents.onPlayerRespawn(newPlayer));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> SanRuntimeEvents.onPlayerLogin(handler.player));
        ServerTickEvents.END_SERVER_TICK.register(SanRuntimeEvents::onServerTick);
        ServerLifecycleEvents.SERVER_STARTED.register(SanRuntimeEvents::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> SanRuntimeEvents.onServerStopped());
    }

}
