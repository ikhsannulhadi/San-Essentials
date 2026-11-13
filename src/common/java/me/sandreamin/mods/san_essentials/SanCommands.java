package me.sandreamin.mods.san_essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Registers San commands and owns their loader-independent runtime behavior.
 */
public final class SanCommands {
    private static final int TELEPORT_SOUND_DELAY_TICKS = 1;
    private static final int MAX_HOME_NAME_LENGTH = 32;
    private static final int RTP_MAX_ATTEMPTS = 12;
    private static final int RTP_FALLBACK_ATTEMPTS = 8;
    private static final int RTP_BORDER_MARGIN = 32;
    private static final int RTP_NETHER_MAX_Y = 126;
    private static final int RTP_SURFACE_SCAN_DEPTH = 8;
    private static final Map<UUID, Integer> RTP_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Integer> TPA_REQUEST_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, String> LAST_COMMANDS = new HashMap<>();
    private static final List<PendingSound> PENDING_SOUNDS = new ArrayList<>();

    private SanCommands() {
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (enabled("tpa")) {
            dispatcher.register(Commands.literal("tpa")
                    .requires(source -> canUse(source, "tpa"))
                    .executes(context -> openTeleportMenu(context.getSource().getPlayerOrException(), TeleportRequestType.TPA))
                    .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> tpa(context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "player")))));
        }
        if (enabled("tpaa")) {
            dispatcher.register(Commands.literal("tpaa")
                    .requires(source -> canUse(source, "tpaa"))
                    .executes(context -> acceptTpa(context.getSource().getPlayerOrException())));
        }
        if (enabled("tpad")) {
            dispatcher.register(Commands.literal("tpad")
                    .requires(source -> canUse(source, "tpad"))
                    .executes(context -> denyTpa(context.getSource().getPlayerOrException())));
        }
        if (enabled("tpaauto")) {
            dispatcher.register(Commands.literal("tpaauto")
                    .requires(source -> canUse(source, "tpaauto"))
                    .executes(context -> tpaAuto(context.getSource().getPlayerOrException())));
        }
        if (enabled("tpa")) {
            registerTpaAlias(dispatcher, "tpaccept", true);
            registerTpaAlias(dispatcher, "tpyes", true);
            registerTpaAlias(dispatcher, "tpdeny", false);
            registerTpaAlias(dispatcher, "tpno", false);
        }
        if (enabled("tpignore")) {
            dispatcher.register(Commands.literal("tpignore")
                    .requires(source -> canUse(source, "tpignore"))
                    .executes(context -> listIgnored(context.getSource().getPlayerOrException()))
                    .then(Commands.literal("clear")
                            .executes(context -> clearIgnored(context.getSource().getPlayerOrException())))
                    .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> toggleIgnored(
                                    context.getSource().getPlayerOrException(),
                                    EntityArgument.getPlayer(context, "player")
                            ))));
        }

        if (enabled("tpahere")) {
            dispatcher.register(Commands.literal("tpahere")
                    .requires(source -> canUse(source, "tpahere"))
                    .executes(context -> openTeleportMenu(context.getSource().getPlayerOrException(), TeleportRequestType.TPAHERE))
                    .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> tpahere(context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "player")))));
        }
        if (enabled("tpaherea")) {
            dispatcher.register(Commands.literal("tpaherea")
                    .requires(source -> canUse(source, "tpaherea"))
                    .executes(context -> acceptTpahere(context.getSource().getPlayerOrException())));
        }
        if (enabled("tpahered")) {
            dispatcher.register(Commands.literal("tpahered")
                    .requires(source -> canUse(source, "tpahered"))
                    .executes(context -> denyTpahere(context.getSource().getPlayerOrException())));
        }

        if (enabled("rtp")) {
            dispatcher.register(Commands.literal("rtp")
                    .requires(source -> canUse(source, "rtp"))
                    .executes(context -> rtp(context.getSource().getPlayerOrException())));
        }
        if (enabled("spawn")) {
            dispatcher.register(Commands.literal("spawn")
                    .requires(source -> canUse(source, "spawn"))
                    .executes(context -> spawn(context.getSource().getPlayerOrException())));
        }
        if (enabled("back")) {
            dispatcher.register(Commands.literal("back")
                    .requires(source -> canUse(source, "back"))
                    .executes(context -> back(context.getSource().getPlayerOrException())));
        }
        if (enabled("dback")) {
            dispatcher.register(Commands.literal("dback")
                    .requires(source -> canUse(source, "dback"))
                    .executes(context -> back(context.getSource().getPlayerOrException())));
        }
        if (enabled("tback")) {
            dispatcher.register(Commands.literal("tback")
                    .requires(source -> canUse(source, "tback"))
                    .executes(context -> tback(context.getSource().getPlayerOrException())));
        }
        if (enabled("sethome")) {
            dispatcher.register(Commands.literal("sethome")
                    .requires(source -> canUse(source, "sethome"))
                    .executes(context -> sethome(context.getSource().getPlayerOrException(), SanPlayerDataStore.DEFAULT_HOME_NAME))
                    .then(Commands.argument("name", StringArgumentType.word())
                            .suggests(SanCommands::suggestOwnHomes)
                            .executes(context -> sethome(
                                    context.getSource().getPlayerOrException(),
                                    StringArgumentType.getString(context, "name")
                            ))));
        }
        if (enabled("delhome")) {
            dispatcher.register(Commands.literal("delhome")
                    .requires(source -> canUse(source, "delhome"))
                    .executes(context -> delhome(context.getSource().getPlayerOrException(), SanPlayerDataStore.DEFAULT_HOME_NAME))
                    .then(Commands.argument("name", StringArgumentType.word())
                            .suggests(SanCommands::suggestOwnHomes)
                            .executes(context -> delhome(
                                    context.getSource().getPlayerOrException(),
                                    StringArgumentType.getString(context, "name")
                            ))));
        }
        if (enabled("home")) {
            dispatcher.register(Commands.literal("home")
                    .requires(source -> canUse(source, "home"))
                    .executes(context -> home(context.getSource().getPlayerOrException(), SanPlayerDataStore.DEFAULT_HOME_NAME))
                    .then(Commands.argument("name", StringArgumentType.word())
                            .suggests(SanCommands::suggestOwnHomes)
                            .executes(context -> home(
                                    context.getSource().getPlayerOrException(),
                                    StringArgumentType.getString(context, "name")
                            ))));
        }
        if (enabled("homes")) {
            dispatcher.register(Commands.literal("homes")
                    .requires(source -> canUse(source, "homes"))
                    .executes(context -> openHomeMenu(context.getSource().getPlayerOrException()))
                    .then(Commands.literal("list")
                            .executes(context -> listHomes(context.getSource().getPlayerOrException()))));
        }
        if (enabled("suicide")) {
            dispatcher.register(Commands.literal("suicide")
                    .requires(source -> canUse(source, "suicide"))
                    .executes(context -> suicide(context.getSource().getPlayerOrException())));
        }
        if (enabled("trash")) {
            dispatcher.register(Commands.literal("trash")
                    .requires(source -> canUse(source, "trash"))
                    .executes(context -> trash(context.getSource().getPlayerOrException())));
        }

        if (enabled("san-ess")) {
            var sanEssential = Commands.literal("san-ess")
                    .requires(source -> canUse(source, "san-ess"))
                    .executes(context -> help(context.getSource().getPlayerOrException()))
                    .then(Commands.literal("help")
                            .executes(context -> help(context.getSource().getPlayerOrException())));
            if (enabled("san-ess-reset")) {
                sanEssential.then(Commands.literal("reset")
                        .requires(source -> canUse(source, "san-ess-reset"))
                        .then(Commands.literal("all")
                                .executes(context -> resetAll(context.getSource().getPlayerOrException())))
                        .then(Commands.literal("notice")
                                .executes(context -> resetNotice(context.getSource().getPlayerOrException()))));
            }
            dispatcher.register(sanEssential);
        }

        if (enabled("warp")) {
            dispatcher.register(Commands.literal("warp")
                    .requires(source -> canUse(source, "warp"))
                    .then(Commands.argument("name", StringArgumentType.word())
                            .suggests(SanCommands::suggestWarps)
                            .executes(context -> warp(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))));
        }
        if (enabled("warps")) {
            dispatcher.register(Commands.literal("warps")
                    .requires(source -> canUse(source, "warps"))
                    .executes(context -> openWarpMenu(context.getSource().getPlayerOrException())));
        }
        if (enabled("setwarp")) {
            dispatcher.register(Commands.literal("setwarp")
                    .requires(source -> canUse(source, "setwarp"))
                    .then(Commands.argument("name", StringArgumentType.word())
                            .suggests(SanCommands::suggestWarps)
                            .executes(context -> setWarp(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))));
        }
        if (enabled("delwarp")) {
            dispatcher.register(Commands.literal("delwarp")
                    .requires(source -> canUse(source, "delwarp"))
                    .then(Commands.argument("name", StringArgumentType.word())
                            .suggests(SanCommands::suggestWarps)
                            .executes(context -> delWarp(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))));
        }
        if (enabled("repeat")) {
            dispatcher.register(Commands.literal("repeat")
                    .requires(source -> canUse(source, "repeat"))
                    .then(Commands.argument("times", IntegerArgumentType.integer(1, 99))
                            .executes(context -> repeat(context.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(context, "times"), null))
                            .then(Commands.argument("command", StringArgumentType.greedyString())
                                    .executes(context -> repeat(
                                            context.getSource().getPlayerOrException(),
                                            IntegerArgumentType.getInteger(context, "times"),
                                            StringArgumentType.getString(context, "command")
                                    )))));
        }
        if (enabled("heal")) {
            dispatcher.register(Commands.literal("heal")
                    .requires(source -> canUse(source, "heal"))
                    .executes(context -> heal(context.getSource().getPlayerOrException(), context.getSource().getPlayerOrException()))
                    .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> heal(context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "player")))));
        }
        if (enabled("feed")) {
            dispatcher.register(Commands.literal("feed")
                    .requires(source -> canUse(source, "feed"))
                    .executes(context -> feed(context.getSource().getPlayerOrException(), context.getSource().getPlayerOrException()))
                    .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> feed(context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "player")))));
        }
        if (enabled("repair")) {
            dispatcher.register(Commands.literal("repair")
                    .requires(source -> canUse(source, "repair"))
                    .executes(context -> repair(context.getSource().getPlayerOrException(), context.getSource().getPlayerOrException()))
                    .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> repair(context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "player")))));
        }
        if (enabled("fly")) {
            dispatcher.register(Commands.literal("fly")
                    .requires(source -> canUse(source, "fly"))
                    .executes(context -> fly(context.getSource().getPlayerOrException(), context.getSource().getPlayerOrException()))
                    .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> fly(context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "player")))));
        }
        if (enabled("god")) {
            dispatcher.register(Commands.literal("god")
                    .requires(source -> canUse(source, "god"))
                    .executes(context -> god(context.getSource().getPlayerOrException(), context.getSource().getPlayerOrException()))
                    .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> god(context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "player")))));
        }
    }

    public static void onServerTick(MinecraftServer server) {
        int tick = server.getTickCount();
        for (TeleportRequestType type : TeleportRequestType.values()) {
            expireRequests(server, tick, type);
        }
        RTP_COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() <= tick);
        TPA_REQUEST_COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() <= tick);
        playPendingSounds(server, tick);
        if (tick % 20 == 0) {
            keepFlyEnabled(server);
        }
    }

    public static void rememberCommand(ServerPlayer player, String command) {
        String normalized = normalizeCommand(command);
        if (!normalized.isBlank() && !rootCommand(normalized).equalsIgnoreCase("repeat")) {
            LAST_COMMANDS.put(player.getUUID(), normalized);
        }
    }

    public static void forgetPlayer(ServerPlayer player) {
        UUID playerId = player.getUUID();
        LAST_COMMANDS.remove(playerId);
        PENDING_SOUNDS.removeIf(sound -> sound.player().equals(playerId));
    }

    public static boolean isFlyEnabled(ServerPlayer player) {
        return SanPlayerData.get(SanCompat.server(player)).isFlyEnabled(player.getUUID());
    }

    public static void restoreFly(ServerPlayer player) {
        if (isFlyEnabled(player) && !SanCompat.hasFlightAbility(player)) {
            setMayFly(player, true);
        }
    }

    public static void restoreFly(ServerPlayer player, boolean wasFlying) {
        if (isFlyEnabled(player)) {
            SanCompat.restoreFlight(player, wasFlying);
        }
    }

    public static void clearRuntimeState() {
        // These collections contain session-only state and must not survive a server restart.
        for (TeleportRequestType type : TeleportRequestType.values()) {
            type.requests().clear();
        }
        RTP_COOLDOWNS.clear();
        TPA_REQUEST_COOLDOWNS.clear();
        LAST_COMMANDS.clear();
        PENDING_SOUNDS.clear();
    }

    private static int tpa(ServerPlayer sender, ServerPlayer target) {
        return sendTeleportRequest(sender, target, TeleportRequestType.TPA);
    }

    private static int openTeleportMenu(ServerPlayer player, TeleportRequestType type) {
        SanCompat.openTeleportMenu(player, type == TeleportRequestType.TPAHERE, 0);
        return 1;
    }

    private static void registerTpaAlias(CommandDispatcher<CommandSourceStack> dispatcher, String alias, boolean accept) {
        if (dispatcher.getRoot().getChild(alias) != null) {
            return;
        }

        dispatcher.register(Commands.literal(alias)
                .requires(source -> canUse(source, alias))
                .executes(context -> accept
                        ? acceptTpa(context.getSource().getPlayerOrException())
                        : denyTpa(context.getSource().getPlayerOrException())));
    }

    private static int acceptTpa(ServerPlayer target) {
        return respondToTeleportRequest(target, TeleportRequestType.TPA, true);
    }

    private static int denyTpa(ServerPlayer target) {
        return respondToTeleportRequest(target, TeleportRequestType.TPA, false);
    }

    private static int tpaAuto(ServerPlayer player) {
        boolean enabled = SanPlayerData.get(SanCompat.server(player)).toggleTpaAuto(player.getUUID());
        send(player, enabled ? "san_essentials.tpaauto.enabled" : "san_essentials.tpaauto.disabled");
        return 1;
    }

    private static int tpahere(ServerPlayer sender, ServerPlayer target) {
        return sendTeleportRequest(sender, target, TeleportRequestType.TPAHERE);
    }

    static List<ServerPlayer> teleportMenuTargets(ServerPlayer player) {
        return SanCompat.server(player).getPlayerList().getPlayers().stream()
                .filter(target -> SanConfig.allowSelfTeleportRequests() || !target.getUUID().equals(player.getUUID()))
                .sorted(Comparator.comparing(SanCompat::profileName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    static boolean requestTeleportFromMenu(ServerPlayer sender, UUID targetId, boolean here) {
        TeleportRequestType type = here ? TeleportRequestType.TPAHERE : TeleportRequestType.TPA;
        if (!commandAvailable(sender, type.languagePrefix)) {
            return false;
        }

        ServerPlayer target = SanCompat.server(sender).getPlayerList().getPlayer(targetId);
        if (target == null) {
            send(sender, "san_essentials.teleport_menu.player.offline");
            return false;
        }

        return sendTeleportRequest(sender, target, type) > 0;
    }

    static boolean canToggleTpaAutoFromMenu(ServerPlayer player) {
        return commandAvailable(player, "tpaauto");
    }

    static boolean isTpaAutoEnabled(ServerPlayer player) {
        return canToggleTpaAutoFromMenu(player)
                && SanPlayerData.get(SanCompat.server(player)).isTpaAuto(player.getUUID());
    }

    static void toggleTpaAutoFromMenu(ServerPlayer player) {
        if (commandAvailable(player, "tpaauto")) {
            tpaAuto(player);
        }
    }

    private static int toggleIgnored(ServerPlayer owner, ServerPlayer target) {
        if (owner.getUUID().equals(target.getUUID())) {
            send(owner, "san_essentials.tpignore.self");
            return 0;
        }

        boolean ignored = SanPlayerData.get(SanCompat.server(owner)).toggleTpIgnore(
                owner.getUUID(), target.getUUID(), SanCompat.profileName(target)
        );
        send(owner, ignored ? "san_essentials.tpignore.added" : "san_essentials.tpignore.removed", target.getName());
        return 1;
    }

    private static int listIgnored(ServerPlayer player) {
        Map<UUID, String> ignored = SanPlayerData.get(SanCompat.server(player)).ignoredPlayers(player.getUUID());
        if (ignored.isEmpty()) {
            send(player, "san_essentials.tpignore.none");
            return 0;
        }

        send(player, "san_essentials.tpignore.header", ignored.size());
        ignored.values().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(name -> send(player, "san_essentials.tpignore.entry", name));
        return 1;
    }

    private static int clearIgnored(ServerPlayer player) {
        SanPlayerData.get(SanCompat.server(player)).clearTpIgnores(player.getUUID());
        send(player, "san_essentials.tpignore.cleared");
        return 1;
    }

    private static int acceptTpahere(ServerPlayer target) {
        return respondToTeleportRequest(target, TeleportRequestType.TPAHERE, true);
    }

    private static int denyTpahere(ServerPlayer target) {
        return respondToTeleportRequest(target, TeleportRequestType.TPAHERE, false);
    }

    private static int sendTeleportRequest(ServerPlayer sender, ServerPlayer target, TeleportRequestType type) {
        if (!SanConfig.allowSelfTeleportRequests() && sender.getUUID().equals(target.getUUID())) {
            send(sender, "san_essentials.teleport.request.self_disabled");
            return 0;
        }

        int tick = SanCompat.server(sender).getTickCount();
        int cooldownExpireTick = TPA_REQUEST_COOLDOWNS.getOrDefault(sender.getUUID(), 0);
        if (cooldownExpireTick > tick) {
            send(sender, "san_essentials.tpa.request.cooldown", ticksToSeconds(cooldownExpireTick - tick));
            return 0;
        }

        if (SanPlayerData.get(SanCompat.server(sender)).isTpIgnored(target.getUUID(), sender.getUUID())) {
            send(sender, "san_essentials.tpignore.sender_ignored", target.getName());
            applyTpaRequestCooldown(sender, tick);
            return 0;
        }

        if (type == TeleportRequestType.TPA
                && commandAvailable(target, "tpaauto")
                && SanPlayerData.get(SanCompat.server(sender)).isTpaAuto(target.getUUID())) {
            teleport(sender, SanCompat.level(target), target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
            notifyParticipants(
                    target, "san_essentials.tpa.auto.target", sender.getName(),
                    sender, "san_essentials.tpa.auto.sender", target.getName()
            );
            teleportSounds(target, sender);
            applyTpaRequestCooldown(sender, tick);
            return 1;
        }

        if (!canRespondToTeleportRequest(target, type)) {
            send(sender, "san_essentials.teleport.request.response_unavailable", target.getName());
            return 0;
        }

        if (type.requests().containsKey(target.getUUID())) {
            send(sender, type.key("request.busy"));
            return 0;
        }

        int timeoutSeconds = SanConfig.teleportRequestTimeoutSeconds();
        type.requests().put(
                target.getUUID(),
                new PendingRequest(sender.getUUID(), tick + secondsToTicks(timeoutSeconds))
        );
        applyTpaRequestCooldown(sender, tick);
        send(target, type.key("request.target"), sender.getName());
        sendRaw(target, prefixedRequestButtons(target, type.acceptCommand(), type.denyCommand()));
        send(target, type.key("request.commands"));
        bell(target);
        if (!sender.getUUID().equals(target.getUUID())) {
            send(sender, type.key("request.sender"), target.getName(), timeoutSeconds);
        }
        return 1;
    }

    private static void applyTpaRequestCooldown(ServerPlayer player, int tick) {
        int cooldownSeconds = SanConfig.tpaRequestCooldownSeconds();
        if (cooldownSeconds > 0) {
            TPA_REQUEST_COOLDOWNS.put(player.getUUID(), tick + secondsToTicks(cooldownSeconds));
        }
    }

    private static int respondToTeleportRequest(ServerPlayer target, TeleportRequestType type, boolean accepted) {
        PendingRequest request = type.requests().remove(target.getUUID());
        if (request == null) {
            send(target, type.key(accepted ? "accept.none" : "deny.none"));
            return 0;
        }

        MinecraftServer server = SanCompat.server(target);
        if (request.expireTick() <= server.getTickCount()) {
            notifyExpiredRequest(server, target.getUUID(), request, type);
            return 0;
        }

        ServerPlayer requester = server.getPlayerList().getPlayer(request.requester());
        if (requester == null) {
            send(target, type.key(accepted ? "accept.none" : "deny.none"));
            return 0;
        }

        String action = accepted ? "accept" : "deny";
        if (accepted) {
            if (type.movesRequester()) {
                teleport(requester, SanCompat.level(target), target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
            } else {
                teleport(target, SanCompat.level(requester), requester.getX(), requester.getY(), requester.getZ(), requester.getYRot(), requester.getXRot());
            }
        }
        notifyParticipants(
                target, type.key(action + ".target"), requester.getName(),
                requester, type.key(action + ".sender"), target.getName()
        );
        if (accepted) {
            teleportSounds(target, requester);
        }
        return 1;
    }

    private static int rtp(ServerPlayer player) {
        int tick = SanCompat.server(player).getTickCount();
        int cooldownExpireTick = RTP_COOLDOWNS.getOrDefault(player.getUUID(), 0);
        if (cooldownExpireTick > tick) {
            send(player, "san_essentials.rtp.cooldown", ticksToSeconds(cooldownExpireTick - tick));
            return 0;
        }

        ServerLevel level = SanCompat.level(player);
        Optional<BlockPos> destination = findRandomTeleportDestination(player, level);
        if (destination.isEmpty()) {
            send(player, "san_essentials.rtp.no_safe_location");
            applyRtpCooldown(player, tick);
            return 0;
        }

        BlockPos target = destination.get();
        teleport(player, level, target.getX() + 0.5, target.getY(), target.getZ() + 0.5, player.getYRot(), player.getXRot());
        send(player, "san_essentials.rtp.success");
        title(player, "san_essentials.rtp.title", "san_essentials.rtp.subtitle");
        teleportSound(player);
        applyRtpCooldown(player, tick);
        return 1;
    }

    private static void applyRtpCooldown(ServerPlayer player, int tick) {
        int cooldownSeconds = SanConfig.rtpCooldownSeconds();
        if (cooldownSeconds > 0) {
            RTP_COOLDOWNS.put(player.getUUID(), tick + secondsToTicks(cooldownSeconds));
        }
    }

    private static Optional<BlockPos> findRandomTeleportDestination(ServerPlayer player, ServerLevel level) {
        WorldBorder border = level.getWorldBorder();
        double borderWidth = border.getMaxX() - border.getMinX();
        double borderDepth = border.getMaxZ() - border.getMinZ();
        double margin = Math.min(RTP_BORDER_MARGIN, Math.min(borderWidth, borderDepth) / 4.0);
        double minX = border.getMinX() + margin;
        double maxX = border.getMaxX() - margin;
        double minZ = border.getMinZ() + margin;
        double maxZ = border.getMaxZ() - margin;
        if (minX >= maxX || minZ >= maxZ) {
            return Optional.empty();
        }

        boolean nether = level.dimension() == Level.NETHER;
        boolean end = level.dimension() == Level.END;
        int minDistance = nether ? SanConfig.rtpNetherMinDistance()
                : end ? SanConfig.rtpEndMinDistance() : SanConfig.rtpMinDistance();
        int maxDistance = nether ? SanConfig.rtpNetherMaxDistance()
                : end ? SanConfig.rtpEndMaxDistance() : SanConfig.rtpMaxDistance();
        double minDistanceSquared = (double) minDistance * minDistance;
        double maxDistanceSquared = (double) maxDistance * maxDistance;

        for (int attempt = 0; attempt < RTP_MAX_ATTEMPTS; attempt++) {
            double angle = player.getRandom().nextDouble() * Math.PI * 2.0;
            double distance = Math.sqrt(minDistanceSquared
                    + player.getRandom().nextDouble() * (maxDistanceSquared - minDistanceSquared));
            double x = player.getX() + Math.cos(angle) * distance;
            double z = player.getZ() + Math.sin(angle) * distance;
            if (x < minX || x >= maxX || z < minZ || z >= maxZ) {
                continue;
            }

            Optional<BlockPos> destination = findSafePosition(level, (int) Math.floor(x), (int) Math.floor(z), nether);
            if (destination.isPresent()) {
                return destination;
            }
        }

        // The configured distance ring may lie entirely outside the border or over unsafe terrain.
        // A safe spot inside the border is still better than leaving the player where they are.
        for (int attempt = 0; attempt < RTP_FALLBACK_ATTEMPTS; attempt++) {
            double x = minX + player.getRandom().nextDouble() * (maxX - minX);
            double z = minZ + player.getRandom().nextDouble() * (maxZ - minZ);
            Optional<BlockPos> destination = findSafePosition(level, (int) Math.floor(x), (int) Math.floor(z), nether);
            if (destination.isPresent()) {
                return destination;
            }
        }

        return Optional.empty();
    }

    private static Optional<BlockPos> findSafePosition(ServerLevel level, int x, int z, boolean nether) {
        int minY = SanCompat.minBuildY(level);
        int maxY = SanCompat.maxBuildY(level) - 2;
        if (nether) {
            maxY = Math.min(maxY, RTP_NETHER_MAX_Y);
        } else {
            // Load the target chunk first; Heightmap returns the minimum build height for
            // chunks that are not loaded yet, which would make every RTP attempt fail.
            level.getBlockState(new BlockPos(x, SanCompat.minBuildY(level), z));
            // Keep surface RTP on the surface instead of descending into caves.
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            maxY = Math.min(maxY, surfaceY + 2);
            minY = Math.max(minY, surfaceY - RTP_SURFACE_SCAN_DEPTH);
        }

        for (int y = maxY; y > minY; y--) {
            BlockPos groundPos = new BlockPos(x, y - 1, z);
            BlockState ground = level.getBlockState(groundPos);
            if (!isSafeGround(level, groundPos, ground)) {
                continue;
            }

            BlockPos feetPos = new BlockPos(x, y, z);
            if (!isClearForFeet(level.getBlockState(feetPos))) {
                continue;
            }

            if (!isClearForFeet(level.getBlockState(feetPos.above()))) {
                continue;
            }

            return Optional.of(feetPos);
        }

        return Optional.empty();
    }

    private static boolean isClearForFeet(BlockState state) {
        if (state.isAir()) {
            return true;
        }

        if (!state.getFluidState().isEmpty()) {
            return false;
        }

        if (state.is(BlockTags.FIRE) || state.is(BlockTags.CAMPFIRES)
                || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.CACTUS)
                || state.is(Blocks.SWEET_BERRY_BUSH)) {
            return false;
        }

        return state.canBeReplaced();
    }

    private static boolean isSafeGround(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }

        if (state.is(BlockTags.FIRE) || state.is(BlockTags.CAMPFIRES) || state.is(BlockTags.LEAVES)
                || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CACTUS)
                || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.POWDER_SNOW)) {
            return false;
        }

        return state.isFaceSturdy(level, pos, Direction.UP) || state.is(Blocks.SNOW);
    }

    private static int spawn(ServerPlayer player) {
        ServerLevel level = SanCompat.spawnLevel(player);
        BlockPos spawn = SanCompat.spawnPos(player);
        teleport(player, level, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, SanCompat.spawnAngle(player), 0.0F);
        send(player, "san_essentials.spawn.success");
        teleportSound(player);
        return 1;
    }

    private static int back(ServerPlayer player) {
        Optional<SanLocation> location = SanPlayerData.get(SanCompat.server(player)).deathBack(player.getUUID());
        if (location.isPresent()) {
            if (!teleport(player, location.get())) {
                return 0;
            }
            send(player, "san_essentials.back.success");
            teleportSound(player);
            return 1;
        }

        send(player, "san_essentials.back.none");
        return 0;
    }

    private static int tback(ServerPlayer player) {
        Optional<SanLocation> location = SanPlayerData.get(SanCompat.server(player)).teleportBack(player.getUUID());
        if (location.isPresent()) {
            if (!teleport(player, location.get())) {
                return 0;
            }
            send(player, "san_essentials.tback.success");
            teleportSound(player);
            return 1;
        }

        send(player, "san_essentials.tback.none");
        return 0;
    }

    private static int sethome(ServerPlayer player, String name) {
        String homeName = validatedHomeName(player, name);
        if (homeName == null) {
            return 0;
        }

        int maxHomes = SanConfig.maxHomes();
        if (!SanPlayerData.get(SanCompat.server(player)).setHome(player.getUUID(), homeName, SanLocation.of(player), maxHomes)) {
            send(player, "san_essentials.sethome.limit", homeName, maxHomes);
            return 0;
        }

        send(player, isDefaultHome(homeName)
                ? "san_essentials.sethome.success"
                : "san_essentials.sethome.success.named", homeName);
        return 1;
    }

    private static int delhome(ServerPlayer player, String name) {
        String homeName = validatedHomeName(player, name);
        if (homeName == null) {
            return 0;
        }

        if (SanPlayerData.get(SanCompat.server(player)).deleteHome(player.getUUID(), homeName)) {
            send(player, isDefaultHome(homeName)
                    ? "san_essentials.delhome.success"
                    : "san_essentials.delhome.success.named", homeName);
            return 1;
        }

        send(player, isDefaultHome(homeName)
                ? "san_essentials.delhome.none"
                : "san_essentials.home.none.named", homeName);
        return 0;
    }

    private static int home(ServerPlayer player, String name) {
        String homeName = validatedHomeName(player, name);
        if (homeName == null) {
            return 0;
        }

        SanPlayerData data = SanPlayerData.get(SanCompat.server(player));
        Optional<SanLocation> location = data.home(player.getUUID(), homeName);
        if (location.isPresent()) {
            if (!teleport(player, location.get())) {
                return 0;
            }
            send(player, isDefaultHome(homeName)
                    ? "san_essentials.home.success"
                    : "san_essentials.home.success.named", homeName);
            title(player, "san_essentials.home.title", "san_essentials.home.subtitle", player.getName());
            playSoundLater(player, SoundEvents.VILLAGER_CELEBRATE, SoundSource.PLAYERS, 1.0F, 1.5F, TELEPORT_SOUND_DELAY_TICKS);
            return 1;
        }

        if (!data.homeNames(player.getUUID()).isEmpty()) {
            sendHomes(player, data, homeName);
            return 0;
        }

        send(player, "san_essentials.home.none");
        return 0;
    }

    private static int openHomeMenu(ServerPlayer player) {
        if (SanPlayerData.get(SanCompat.server(player)).homeNames(player.getUUID()).isEmpty()) {
            send(player, "san_essentials.homes.none");
            return 0;
        }

        SanCompat.openHomeMenu(player, 0);
        return 1;
    }

    private static int listHomes(ServerPlayer player) {
        SanPlayerData data = SanPlayerData.get(SanCompat.server(player));
        if (data.homeNames(player.getUUID()).isEmpty()) {
            send(player, "san_essentials.homes.none");
            return 0;
        }

        sendHomes(player, data, null);
        return 1;
    }

    static boolean requestHomeFromMenu(ServerPlayer player, String name) {
        return home(player, name) > 0;
    }

    private static void sendHomes(ServerPlayer player, SanPlayerData data, String missingHome) {
        if (missingHome != null) {
            send(player, "san_essentials.home.none.named", missingHome);
        }

        List<String> names = new ArrayList<>(data.homeNames(player.getUUID()));
        names.sort(String.CASE_INSENSITIVE_ORDER);

        MutableComponent line = SanLang.component(player, "san_essentials.homes.header", names.size(), SanConfig.maxHomes());
        for (String name : names) {
            line.append(Component.literal(" ")).append(SanLang.component(player, "san_essentials.homes.entry", name)
                    .withStyle(Style.EMPTY
                            .withClickEvent(SanCompat.runCommandClick("/home " + name))
                            .withHoverEvent(SanCompat.showTextHover(SanLang.component(player, "san_essentials.homes.entry.tooltip")))));
        }
        sendRaw(player, SanLang.prefixed(line));
    }

    private static String validatedHomeName(ServerPlayer player, String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9_]{1," + MAX_HOME_NAME_LENGTH + "}")) {
            send(player, "san_essentials.home.name.invalid", name);
            return null;
        }
        return normalized;
    }

    private static boolean isDefaultHome(String name) {
        return SanPlayerDataStore.DEFAULT_HOME_NAME.equals(name);
    }

    private static int suicide(ServerPlayer player) {
        SanRuntimeEvents.allowNextDamage(player);
        try {
            SanCompat.kill(player);
        } finally {
            SanRuntimeEvents.clearForcedDamage(player);
        }
        if (!player.isDeadOrDying()) {
            return 0;
        }
        for (ServerPlayer target : SanCompat.server(player).getPlayerList().getPlayers()) {
            send(target, "san_essentials.suicide.broadcast", player.getName());
        }
        return 1;
    }

    private static int trash(ServerPlayer player) {
        MenuProvider menu = new SimpleMenuProvider(
                (containerId, inventory, p) -> ChestMenu.sixRows(containerId, inventory),
                SanLang.component(player, "san_essentials.trash.title")
        );
        player.openMenu(menu);
        SanCompat.playSound(player, SoundEvents.BARREL_OPEN, SoundSource.PLAYERS, 1.0F, 1.0F);
        return 1;
    }

    private static int warp(ServerPlayer player, String name) {
        Optional<SanLocation> location = SanPlayerData.get(SanCompat.server(player)).warp(name);
        if (location.isPresent()) {
            if (!teleport(player, location.get())) {
                return 0;
            }
            send(player, "san_essentials.warp.success", name);
            teleportSound(player);
            return 1;
        }

        send(player, "san_essentials.warp.none", name);
        return 0;
    }

    private static int openWarpMenu(ServerPlayer player) {
        if (SanPlayerData.get(SanCompat.server(player)).warpNames().isEmpty()) {
            send(player, "san_essentials.warps.none");
            return 0;
        }

        SanCompat.openWarpMenu(player, 0);
        return 1;
    }

    static boolean requestWarpFromMenu(ServerPlayer player, String name) {
        return warp(player, name) > 0;
    }

    private static int setWarp(ServerPlayer player, String name) {
        SanPlayerData.get(SanCompat.server(player)).setWarp(name, SanLocation.of(player));
        send(player, "san_essentials.setwarp.success", name);
        return 1;
    }

    private static int delWarp(ServerPlayer player, String name) {
        if (SanPlayerData.get(SanCompat.server(player)).deleteWarp(name)) {
            send(player, "san_essentials.delwarp.success", name);
            return 1;
        }

        send(player, "san_essentials.delwarp.none", name);
        return 0;
    }

    private static int help(ServerPlayer player) {
        send(player, "san_essentials.help.header");
        CommandSourceStack source = player.createCommandSourceStack();
        sendHelpLine(player, source, "san_essentials.help.teleport",
                helpCommand("tpa"), helpCommand("tpahere"), helpCommand("tpaa"), helpCommand("tpad"), helpCommand("tpaauto"),
                helpCommand("tpaherea"), helpCommand("tpahered"), helpCommand("tpignore"),
                helpCommand("back"), helpCommand("dback"), helpCommand("tback"), helpCommand("spawn"), helpCommand("rtp"));
        sendHelpLine(player, source, "san_essentials.help.home",
                helpCommand("home"), helpCommand("homes"), helpCommand("sethome"), helpCommand("delhome"),
                helpCommand("warp"), helpCommand("warps"));
        sendHelpLine(player, source, "san_essentials.help.utility",
                helpCommand("suicide"), helpCommand("trash"));
        sendHelpLine(player, source, "san_essentials.help.op",
                helpCommand("setwarp"), helpCommand("delwarp"), helpCommand("heal"), helpCommand("feed"), helpCommand("repair"),
                helpCommand("fly"), helpCommand("god"), helpCommand("repeat"),
                new HelpCommand("san-ess-reset", "/san-ess reset"));
        return 1;
    }

    private static void sendHelpLine(ServerPlayer player, CommandSourceStack source, String titleKey, HelpCommand... commands) {
        MutableComponent line = SanLang.component(player, titleKey);
        for (int i = 0; i < commands.length; i++) {
            if (i > 0) {
                line.append(Component.literal(" "));
            }

            HelpCommand command = commands[i];
            line.append(Component.literal((canShowAsAvailable(source, command.configKey()) ? "§6" : "§c") + command.display()));
        }
        sendRaw(player, SanLang.prefixed(line));
    }

    private static HelpCommand helpCommand(String command) {
        return new HelpCommand(command, "/" + command);
    }

    private static boolean canShowAsAvailable(CommandSourceStack source, String command) {
        return enabled(command) && canUse(source, command);
    }

    private static int resetAll(ServerPlayer player) {
        SanPlayerData.get(SanCompat.server(player)).clearPlayerData();
        clearRuntimeState();
        SanRuntimeEvents.clearNoticeQueues();
        for (ServerPlayer target : SanCompat.server(player).getPlayerList().getPlayers()) {
            setMayFly(target, target.gameMode.getGameModeForPlayer() == GameType.CREATIVE);
        }
        send(player, "san_essentials.reset.all.success");
        return 1;
    }

    private static int resetNotice(ServerPlayer player) {
        SanPlayerData.get(SanCompat.server(player)).clearNoticeData();
        SanRuntimeEvents.clearNoticeQueues();
        send(player, "san_essentials.reset.notice.success");
        return 1;
    }

    private static int repeat(ServerPlayer player, int times, String command) {
        String normalized = command == null ? LAST_COMMANDS.get(player.getUUID()) : normalizeCommand(command);
        if (normalized == null || normalized.isBlank()) {
            send(player, "san_essentials.repeat.none");
            return 0;
        }
        if (rootCommand(normalized).equalsIgnoreCase("repeat")) {
            send(player, "san_essentials.repeat.blocked");
            return 0;
        }

        CommandSourceStack source = player.createCommandSourceStack().withSuppressedOutput();
        for (int i = 0; i < times; i++) {
            SanCompat.server(player).getCommands().performPrefixedCommand(source, normalized);
        }
        return times;
    }

    private static int heal(ServerPlayer sender, ServerPlayer target) {
        target.setHealth(target.getMaxHealth());
        FoodData food = target.getFoodData();
        food.setFoodLevel(20);
        food.setSaturation(20.0F);
        send(sender, "san_essentials.heal.success", target.getName());
        if (sender != target) {
            send(target, "san_essentials.heal.target", sender.getName());
        }
        return 1;
    }

    private static int feed(ServerPlayer sender, ServerPlayer target) {
        FoodData food = target.getFoodData();
        food.setFoodLevel(20);
        food.setSaturation(20.0F);
        send(sender, "san_essentials.feed.success", target.getName());
        if (sender != target) {
            send(target, "san_essentials.feed.target", sender.getName());
        }
        return 1;
    }

    private static int repair(ServerPlayer sender, ServerPlayer target) {
        ItemStack held = target.getMainHandItem();
        if (held.isEmpty() || !held.isDamageableItem() || !held.isDamaged()) {
            send(sender, "san_essentials.repair.invalid", target.getName());
            return 0;
        }

        held.setDamageValue(0);
        target.setItemInHand(InteractionHand.MAIN_HAND, held);
        send(sender, "san_essentials.repair.success", target.getName());
        if (sender != target) {
            send(target, "san_essentials.repair.target", sender.getName());
        }
        return 1;
    }

    private static int fly(ServerPlayer sender, ServerPlayer target) {
        boolean enabled = SanPlayerData.get(SanCompat.server(sender)).toggleFly(target.getUUID());
        setMayFly(target, enabled || target.gameMode.getGameModeForPlayer() == GameType.CREATIVE);
        send(sender, enabled ? "san_essentials.fly.enabled" : "san_essentials.fly.disabled", target.getName());
        if (sender != target) {
            send(target, enabled ? "san_essentials.fly.target.enabled" : "san_essentials.fly.target.disabled", sender.getName());
        }
        return 1;
    }

    private static int god(ServerPlayer sender, ServerPlayer target) {
        boolean enabled = SanPlayerData.get(SanCompat.server(sender)).toggleGod(target.getUUID());
        send(sender, enabled ? "san_essentials.god.enabled" : "san_essentials.god.disabled", target.getName());
        if (sender != target) {
            send(target, enabled ? "san_essentials.god.target.enabled" : "san_essentials.god.target.disabled", sender.getName());
        }
        return 1;
    }

    private static void expireRequests(MinecraftServer server, int tick, TeleportRequestType type) {
        Iterator<Map.Entry<UUID, PendingRequest>> iterator = type.requests().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingRequest> entry = iterator.next();
            if (entry.getValue().expireTick() > tick) {
                continue;
            }

            iterator.remove();
            notifyExpiredRequest(server, entry.getKey(), entry.getValue(), type);
        }
    }

    private static void notifyExpiredRequest(
            MinecraftServer server,
            UUID targetId,
            PendingRequest request,
            TeleportRequestType type
    ) {
        ServerPlayer target = server.getPlayerList().getPlayer(targetId);
        ServerPlayer requester = server.getPlayerList().getPlayer(request.requester());
        if (target != null) {
            send(target, type.key("timeout.target"));
        }
        if (requester != null && !requester.getUUID().equals(targetId)) {
            send(requester, type.key("timeout.sender"));
        }
    }

    private static boolean teleport(ServerPlayer player, SanLocation location) {
        Optional<ServerLevel> level = location.resolve(SanCompat.server(player));
        if (level.isEmpty()) {
            send(player, "san_essentials.teleport.missing_dimension");
            return false;
        }

        if (!level.get().getWorldBorder().isWithinBounds(location.x(), location.z())) {
            send(player, "san_essentials.teleport.outside_border");
            return false;
        }

        teleport(player, level.get(), location.x(), location.y(), location.z(), location.yRot(), location.xRot());
        return true;
    }

    private static void teleport(ServerPlayer player, ServerLevel level, double x, double y, double z, float yRot, float xRot) {
        SanPlayerData.get(SanCompat.server(player)).setTeleportBack(player.getUUID(), SanLocation.of(player));
        SanCompat.teleport(player, level, x, y, z, yRot, xRot);
    }

    private static MutableComponent prefixedRequestButtons(ServerPlayer player, String acceptCommand, String denyCommand) {
        MutableComponent message = Component.empty();
        message.append(SanLang.component(player, "san_essentials.request.accept")
                .withStyle(Style.EMPTY
                        .withClickEvent(SanCompat.runCommandClick("/" + acceptCommand))
                        .withHoverEvent(SanCompat.showTextHover(SanLang.component(player, "san_essentials.request.accept.tooltip")))));
        message.append(Component.literal(" "));
        message.append(SanLang.component(player, "san_essentials.request.deny")
                .withStyle(Style.EMPTY
                        .withClickEvent(SanCompat.runCommandClick("/" + denyCommand))
                        .withHoverEvent(SanCompat.showTextHover(SanLang.component(player, "san_essentials.request.deny.tooltip")))));
        return SanLang.prefixed(message);
    }

    private static boolean canRespondToTeleportRequest(ServerPlayer target, TeleportRequestType type) {
        return commandAvailable(target, type.acceptCommand())
                && commandAvailable(target, type.denyCommand());
    }

    private static boolean commandAvailable(ServerPlayer player, String command) {
        CommandSourceStack source = player.createCommandSourceStack();
        return enabled(command)
                && SanCompat.server(player).getCommands().getDispatcher().getRoot().getChild(command) != null
                && canUse(source, command);
    }

    private static void notifyParticipants(
            ServerPlayer first,
            String firstKey,
            Object firstArgument,
            ServerPlayer second,
            String secondKey,
            Object secondArgument
    ) {
        send(first, firstKey, firstArgument);
        if (!first.getUUID().equals(second.getUUID())) {
            send(second, secondKey, secondArgument);
        }
    }

    private static void teleportSounds(ServerPlayer first, ServerPlayer second) {
        teleportSound(first);
        if (!first.getUUID().equals(second.getUUID())) {
            teleportSound(second);
        }
    }

    private static void send(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(SanLang.prefixedComponent(player, key, args));
    }

    private static void sendRaw(ServerPlayer player, Component component) {
        player.sendSystemMessage(component);
    }

    private static void bell(ServerPlayer player) {
        SanCompat.playSound(player, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void teleportSound(ServerPlayer player) {
        playSoundLater(player, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F, TELEPORT_SOUND_DELAY_TICKS);
    }

    private static void playSoundLater(ServerPlayer player, SoundEvent sound, SoundSource source, float volume, float pitch, int delayTicks) {
        PENDING_SOUNDS.add(new PendingSound(
                player.getUUID(),
                sound,
                source,
                volume,
                pitch,
                SanCompat.server(player).getTickCount() + delayTicks
        ));
    }

    private static void playPendingSounds(MinecraftServer server, int tick) {
        Iterator<PendingSound> iterator = PENDING_SOUNDS.iterator();
        while (iterator.hasNext()) {
            PendingSound sound = iterator.next();
            if (sound.playTick() > tick) {
                continue;
            }

            iterator.remove();
            ServerPlayer player = server.getPlayerList().getPlayer(sound.player());
            if (player != null) {
                SanCompat.playSound(player, sound.sound(), sound.source(), sound.volume(), sound.pitch());
            }
        }
    }

    private static CompletableFuture<Suggestions> suggestWarps(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(SanPlayerData.get(context.getSource().getServer()).warpNames(), builder);
    }

    private static CompletableFuture<Suggestions> suggestOwnHomes(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        return SharedSuggestionProvider.suggest(SanPlayerData.get(context.getSource().getServer()).homeNames(player.getUUID()), builder);
    }

    private static boolean enabled(String command) {
        return SanConfig.commandEnabled(command);
    }

    private static boolean canUse(CommandSourceStack source, String command) {
        return SanCompat.hasPermission(source, SanConfig.commandPermission(command));
    }

    private static void keepFlyEnabled(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            restoreFly(player);
        }
    }

    private static void setMayFly(ServerPlayer player, boolean mayFly) {
        SanCompat.setMayFly(player, mayFly);
    }

    private static String normalizeCommand(String command) {
        String normalized = command.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1).trim();
        }
        return normalized;
    }

    private static String rootCommand(String command) {
        int space = command.indexOf(' ');
        return space == -1 ? command : command.substring(0, space);
    }

    private static int secondsToTicks(int seconds) {
        return seconds * 20;
    }

    private static int ticksToSeconds(int ticks) {
        return Math.max(1, (ticks + 19) / 20);
    }

    private static void title(ServerPlayer player, String titleKey, String subtitleKey, Object... subtitleArgs) {
        player.connection.send(new ClientboundSetTitleTextPacket(SanLang.component(player, titleKey)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(SanLang.component(player, subtitleKey, subtitleArgs)));
    }

    private record HelpCommand(String configKey, String display) {
    }

    private enum TeleportRequestType {
        TPA("tpa", "tpaa", "tpad", true),
        TPAHERE("tpahere", "tpaherea", "tpahered", false);

        private final String languagePrefix;
        private final String acceptCommand;
        private final String denyCommand;
        private final boolean movesRequester;
        private final Map<UUID, PendingRequest> requests = new HashMap<>();

        TeleportRequestType(String languagePrefix, String acceptCommand, String denyCommand, boolean movesRequester) {
            this.languagePrefix = languagePrefix;
            this.acceptCommand = acceptCommand;
            this.denyCommand = denyCommand;
            this.movesRequester = movesRequester;
        }

        private String key(String suffix) {
            return "san_essentials." + languagePrefix + "." + suffix;
        }

        private String acceptCommand() {
            return acceptCommand;
        }

        private String denyCommand() {
            return denyCommand;
        }

        private boolean movesRequester() {
            return movesRequester;
        }

        private Map<UUID, PendingRequest> requests() {
            return requests;
        }
    }

    private record PendingRequest(UUID requester, int expireTick) {
    }

    private record PendingSound(UUID player, SoundEvent sound, SoundSource source, float volume, float pitch, int playTick) {
    }
}
