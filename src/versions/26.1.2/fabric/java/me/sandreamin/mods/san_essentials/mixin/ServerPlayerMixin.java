package me.sandreamin.mods.san_essentials.mixin;

import me.sandreamin.mods.san_essentials.SanCompat;
import me.sandreamin.mods.san_essentials.SanLocation;
import me.sandreamin.mods.san_essentials.SanPlayerData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    @Inject(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FFZ)Z", at = @At("HEAD"))
    private void sanEssentials$recordTeleportBack(ServerLevel level, double x, double y, double z, Set<?> relativeMovements, float yRot, float xRot, boolean resetCamera, CallbackInfoReturnable<Boolean> callback) {
        sanEssentials$recordTeleportBack();
    }

    private void sanEssentials$recordTeleportBack() {
        ServerPlayer player = (ServerPlayer) (Object) this;
        SanPlayerData.get(SanCompat.server(player)).setTeleportBack(player.getUUID(), SanLocation.of(player));
    }
}
