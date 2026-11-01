package me.sandreamin.mods.san_essentials.mixin;

import com.mojang.brigadier.ParseResults;
import me.sandreamin.mods.san_essentials.SanCommands;
import me.sandreamin.mods.san_essentials.SanCompat;
import me.sandreamin.mods.san_essentials.SanConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public class CommandsMixin {
    @Inject(method = "performCommand", at = @At("HEAD"))
    private void sanEssentials$rememberCommand(ParseResults<CommandSourceStack> parseResults, String command, CallbackInfo callback) {
        CommandSourceStack source = parseResults.getContext().getSource();
        if (SanConfig.commandEnabled("repeat")
                && source.getEntity() instanceof ServerPlayer player
                && SanCompat.hasPermission(player, SanConfig.commandPermission("repeat"))) {
            SanCommands.rememberCommand(player, command);
        }
    }
}
