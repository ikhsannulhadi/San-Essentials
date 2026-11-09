package me.sandreamin.mods.san_essentials;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(SanEssentials.MODID)
public class SanEssentials {
    public static final String MODID = "san_essentials";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SanEssentials(IEventBus modEventBus, ModContainer modContainer) {
        SanStartupBanner.logLoading();
        NeoForge.EVENT_BUS.register(SanNeoForgeCommands.class);
        NeoForge.EVENT_BUS.register(SanEvents.class);
        SanConfig.load();
        modContainer.registerConfig(ModConfig.Type.COMMON, SanConfigBackend.SPEC);
        SanStartupBanner.logLoaded();
    }
}
