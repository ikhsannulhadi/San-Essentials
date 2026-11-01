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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {
    @Shadow
    private ServerPlayer player;

    @Unique
    private boolean sanEssentials$wasFlying;

    @Inject(method = "changeGameModeForPlayer", at = @At("HEAD"))
    private void sanEssentials$captureFlightState(GameType gameType, CallbackInfoReturnable<Boolean> callback) {
        sanEssentials$wasFlying = SanCommands.isFlyEnabled(player) && SanCompat.isFlying(player);
    }

    @Inject(method = "changeGameModeForPlayer", at = @At("RETURN"))
    private void sanEssentials$restoreFlightState(GameType gameType, CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValue()) {
            SanCommands.restoreFly(player, sanEssentials$wasFlying);
        }
    }
}
