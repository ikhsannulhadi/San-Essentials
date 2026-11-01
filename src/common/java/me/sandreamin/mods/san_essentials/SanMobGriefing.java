package me.sandreamin.mods.san_essentials;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.level.Explosion;

/**
 * Centralizes the loader-neutral checks used to suppress configured mob block damage.
 */
public final class SanMobGriefing {
    private SanMobGriefing() {
    }

    public static boolean shouldPreventBlockGriefing(Entity entity) {
        return entity instanceof Creeper && SanConfig.preventCreeperBlockDamage()
                || entity instanceof EnderMan && SanConfig.preventEndermanBlockDamage()
                || entity instanceof Ravager && SanConfig.preventRavagerBlockDamage();
    }

    public static boolean shouldPreventCreeperExplosionBlocks(Explosion explosion) {
        return SanConfig.preventCreeperBlockDamage() && explosion.getDirectSourceEntity() instanceof Creeper;
    }
}
