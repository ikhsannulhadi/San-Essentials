package me.sandreamin.mods.san_essentials.mixin;

import me.sandreamin.mods.san_essentials.SanCommands;
import me.sandreamin.mods.san_essentials.SanCompat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerAbilityRefreshMixin {
    @Shadow
    private ServerPlayer player;

    @Unique
    private boolean sanEssentials$wasFlying;

    @Inject(method = "setGameModeForPlayer", at = @At("HEAD"))
    private void sanEssentials$captureFlightState(GameType gameType, GameType previousGameType, CallbackInfo callback) {
        sanEssentials$wasFlying = SanCommands.isFlyEnabled(player) && SanCompat.isFlying(player);
    }

    @Inject(method = "setGameModeForPlayer", at = @At("RETURN"))
    private void sanEssentials$restoreFlightState(GameType gameType, GameType previousGameType, CallbackInfo callback) {
        if (SanCommands.isFlyEnabled(player)) {
            SanCompat.restoreFlightSilently(player, sanEssentials$wasFlying);
        }
    }
}
