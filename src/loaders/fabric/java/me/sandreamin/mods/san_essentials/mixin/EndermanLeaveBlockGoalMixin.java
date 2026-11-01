package me.sandreamin.mods.san_essentials.mixin;

import me.sandreamin.mods.san_essentials.SanMobGriefing;
import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.entity.monster.EnderMan$EndermanLeaveBlockGoal")
public class EndermanLeaveBlockGoalMixin {
    @Shadow
    @Final
    private EnderMan enderman;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void sanEssentials$preventBlockPlacement(CallbackInfoReturnable<Boolean> cir) {
        if (SanMobGriefing.shouldPreventBlockGriefing(this.enderman)) {
            cir.setReturnValue(false);
        }
    }
}
