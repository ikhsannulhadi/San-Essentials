package me.sandreamin.mods.san_essentials;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class SanNeoForgeCommands {
    private SanNeoForgeCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        SanCommands.registerCommands(event.getDispatcher());
    }
}
