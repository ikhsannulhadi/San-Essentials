package me.sandreamin.mods.san_essentials.mixin;

import me.sandreamin.mods.san_essentials.SanMobGriefing;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Creeper.class)
public class CreeperMixin {
    @ModifyArg(
            method = "explodeCreeper",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;DDDFLnet/minecraft/world/level/Level$ExplosionInteraction;)Lnet/minecraft/world/level/Explosion;"
            ),
            index = 5,
            require = 0
    )
    @Group(name = "sanEssentials$preventCreeperBlockDamage", min = 1)
    private Level.ExplosionInteraction sanEssentials$preventBlockDamage(Level.ExplosionInteraction interaction) {
        return sanEssentials$blockDamageInteraction(interaction);
    }

    @ModifyArg(
            method = "explodeCreeper",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;explode(Lnet/minecraft/world/entity/Entity;DDDFLnet/minecraft/world/level/Level$ExplosionInteraction;)V"
            ),
            index = 5,
            require = 0
    )
    @Group(name = "sanEssentials$preventCreeperBlockDamage", min = 1)
    private Level.ExplosionInteraction sanEssentials$preventBlockDamageModern(Level.ExplosionInteraction interaction) {
        return sanEssentials$blockDamageInteraction(interaction);
    }

    private Level.ExplosionInteraction sanEssentials$blockDamageInteraction(Level.ExplosionInteraction interaction) {
        if (SanMobGriefing.shouldPreventBlockGriefing((Creeper) (Object) this)) {
            return Level.ExplosionInteraction.NONE;
        }
        return interaction;
    }
}
