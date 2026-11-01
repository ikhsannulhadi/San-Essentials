package me.sandreamin.mods.san_essentials;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SanEssentials implements ModInitializer {
    public static final String MODID = "san_essentials";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    @Override
    public void onInitialize() {
        SanConfig.load();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> SanCommands.registerCommands(dispatcher));
        SanEvents.register();
    }
}
