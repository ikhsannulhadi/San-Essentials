package me.sandreamin.mods.san_essentials;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Defines the shared configuration schema exposed by both loader backends.
 */
public final class SanConfig {
    static final String FILE_NAME = "san_essentials-common.toml";
    private static final String DEFAULT_VALUE_COMMENT_PREFIX = " Default: ";

    private static final Set<String> SUPPORTED_LANGUAGES = Set.of(
            "en_us", "id_id", "zh_cn", "zh_tw", "zh_hk", "ru_ru", "de_de", "fr_fr", "ja_jp", "ko_kr"
    );

    private static final ValueDefinition FALLBACK_LANGUAGE = stringValue(
            "fallbackLanguage",
            "en_us",
            SUPPORTED_LANGUAGES,
            " Fallback language used by the server when the player's client language is unsupported.",
            " This prevents clients without the mod installed from seeing untranslated localization keys.",
            " Supported values: en_us, id_id, zh_cn, zh_tw, zh_hk, ru_ru, de_de, fr_fr, ja_jp, ko_kr"
    );
    private static final ValueDefinition MESSAGE_PREFIX_ENABLED = booleanValue(
            "messagePrefixEnabled",
            false,
            " Whether mod messages should include the [SE] prefix."
    );
    private static final ValueDefinition FIRST_JOIN_NOTICE = booleanValue(
            "firstJoinNotice",
            true,
            " Whether San Essentials should show a one-time command notice when a player first joins the server."
    );
    private static final ValueDefinition PREVENT_CREEPER_BLOCK_DAMAGE = booleanValue(
            "preventCreeperBlockDamage",
            true,
            " Whether creepers are prevented from breaking blocks with explosions."
    );
    private static final ValueDefinition PREVENT_ENDERMAN_BLOCK_DAMAGE = booleanValue(
            "preventEndermanBlockDamage",
            true,
            " Whether endermen are prevented from picking up or placing blocks."
    );
    private static final ValueDefinition PREVENT_RAVAGER_BLOCK_DAMAGE = booleanValue(
            "preventRavagerBlockDamage",
            true,
            " Whether ravagers are prevented from breaking leaves and crops."
    );
    private static final ValueDefinition MAX_HOMES = intValue(
            "maxHomes",
            5,
            1,
            10,
            " How many named homes each player may save with /sethome."
    );
    private static final ValueDefinition RTP_COOLDOWN_SECONDS = intValue(
            "rtpCooldownSeconds",
            30,
            0,
            86400,
            " Cooldown in seconds for /rtp. Use 0 to disable the cooldown."
    );
    private static final ValueDefinition RTP_MIN_DISTANCE = intValue(
            "rtpMinDistance",
            2000,
            0,
            30000000,
            " Minimum distance from the current position for /rtp in normal dimensions."
    );
    private static final ValueDefinition RTP_MAX_DISTANCE = intValue(
            "rtpMaxDistance",
            5000,
            1,
            30000000,
            " Maximum distance from the current position for /rtp in normal dimensions."
    );
    private static final ValueDefinition RTP_NETHER_MIN_DISTANCE = intValue(
            "rtpNetherMinDistance",
            600,
            0,
            30000000,
            " Minimum distance from the current position for /rtp in the Nether."
    );
    private static final ValueDefinition RTP_NETHER_MAX_DISTANCE = intValue(
            "rtpNetherMaxDistance",
            1500,
            1,
            30000000,
            " Maximum distance from the current position for /rtp in the Nether."
    );
    private static final ValueDefinition RTP_END_MIN_DISTANCE = intValue(
            "rtpEndMinDistance",
            -1,
            -1,
            30000000,
            " Minimum distance from the current position for /rtp in the End.",
            " Use -1 to reuse rtpMinDistance."
    );
    private static final ValueDefinition RTP_END_MAX_DISTANCE = intValue(
            "rtpEndMaxDistance",
            -1,
            -1,
            30000000,
            " Maximum distance from the current position for /rtp in the End.",
            " Use -1 to reuse rtpMaxDistance."
    );
    private static final ValueDefinition TELEPORT_REQUEST_TIMEOUT_SECONDS = intValue(
            "teleportRequestTimeoutSeconds",
            60,
            1,
            86400,
            " How long TPA and TPAhere requests stay valid, in seconds."
    );
    private static final ValueDefinition ALLOW_SELF_TELEPORT_REQUESTS = booleanValue(
            "allowSelfTeleportRequests",
            true,
            " Whether players may send /tpa and /tpahere requests to themselves."
    );
    private static final ValueDefinition TPA_REQUEST_COOLDOWN_SECONDS = intValue(
            "tpaRequestCooldownSeconds",
            5,
            0,
            86400,
            " Cooldown in seconds between TPA and TPAhere requests. Use 0 to disable the cooldown."
    );

    private static final List<ValueDefinition> VALUES = List.of(
            FALLBACK_LANGUAGE,
            MESSAGE_PREFIX_ENABLED,
            FIRST_JOIN_NOTICE,
            PREVENT_CREEPER_BLOCK_DAMAGE,
            PREVENT_ENDERMAN_BLOCK_DAMAGE,
            PREVENT_RAVAGER_BLOCK_DAMAGE,
            MAX_HOMES,
            RTP_COOLDOWN_SECONDS,
            RTP_MIN_DISTANCE,
            RTP_MAX_DISTANCE,
            RTP_NETHER_MIN_DISTANCE,
            RTP_NETHER_MAX_DISTANCE,
            RTP_END_MIN_DISTANCE,
            RTP_END_MAX_DISTANCE,
            TELEPORT_REQUEST_TIMEOUT_SECONDS,
            ALLOW_SELF_TELEPORT_REQUESTS,
            TPA_REQUEST_COOLDOWN_SECONDS
    );
    private static final Set<String> VALUE_KEYS = Set.copyOf(
            VALUES.stream().map(ValueDefinition::key).toList()
    );

    private static final List<String> COMMAND_COMMENTS = List.of(
            " Command settings.",
            " enabled: whether the command is registered when the server starts. Changes require a server restart.",
            " permissionLevel: required OP permission level. Use 0 to allow everyone. Changes apply without restart.",
            " Derived commands are not configured separately: tpaa, tpad, tpaauto, tpaccept, tpyes, tpdeny, tpno follow tpa;",
            " tpaherea and tpahered follow tpahere; dback follows back; warps follows warp.",
            " TPA and TPAhere requests require the target to have access to both matching accept and deny commands.",
            " Stored TPA auto-accept state only applies while the tpaauto command is enabled and accessible."
    );

    private static final List<CommandDefinition> COMMANDS = List.of(
            command("tpa", true, 0),
            command("tpahere", true, 0),
            command("rtp", true, 0),
            command("spawn", true, 0),
            command("back", true, 0),
            command("tback", true, 0),
            command("sethome", true, 0),
            command("delhome", true, 0),
            command("home", true, 0),
            command("homes", true, 0),
            command("suicide", true, 0),
            command("trash", true, 0),
            command("san-ess", true, 0),
            command("san-ess-reset", true, 4),
            command("warp", true, 0),
            command("setwarp", true, 4),
            command("delwarp", true, 4),
            command("tpignore", true, 0),
            command("repeat", true, 4),
            command("heal", true, 4),
            command("feed", true, 4),
            command("repair", true, 4),
            command("fly", true, 4),
            command("god", true, 4)
    );
    private static final Set<String> COMMAND_NAMES = Set.copyOf(
            COMMANDS.stream().map(CommandDefinition::name).toList()
    );

    private static final Map<String, String> DERIVED_COMMANDS = Map.ofEntries(
            Map.entry("tpaa", "tpa"),
            Map.entry("tpad", "tpa"),
            Map.entry("tpaauto", "tpa"),
            Map.entry("tpaccept", "tpa"),
            Map.entry("tpyes", "tpa"),
            Map.entry("tpdeny", "tpa"),
            Map.entry("tpno", "tpa"),
            Map.entry("tpaherea", "tpahere"),
            Map.entry("tpahered", "tpahere"),
            Map.entry("dback", "back"),
            Map.entry("warps", "warp")
    );

    private SanConfig() {
    }

    public static void load() {
        SanConfigBackend.load();
    }

    public static String fallbackLanguage() {
        return SanConfigBackend.stringValue(FALLBACK_LANGUAGE);
    }

    public static boolean messagePrefixEnabled() {
        return SanConfigBackend.booleanValue(MESSAGE_PREFIX_ENABLED);
    }

    public static boolean firstJoinNotice() {
        return SanConfigBackend.booleanValue(FIRST_JOIN_NOTICE);
    }

    public static boolean preventCreeperBlockDamage() {
        return SanConfigBackend.booleanValue(PREVENT_CREEPER_BLOCK_DAMAGE);
    }

    public static boolean preventEndermanBlockDamage() {
        return SanConfigBackend.booleanValue(PREVENT_ENDERMAN_BLOCK_DAMAGE);
    }

    public static boolean preventRavagerBlockDamage() {
        return SanConfigBackend.booleanValue(PREVENT_RAVAGER_BLOCK_DAMAGE);
    }

    public static int maxHomes() {
        return SanConfigBackend.intValue(MAX_HOMES);
    }

    public static int rtpCooldownSeconds() {
        return SanConfigBackend.intValue(RTP_COOLDOWN_SECONDS);
    }

    public static int rtpMinDistance() {
        return SanConfigBackend.intValue(RTP_MIN_DISTANCE);
    }

    public static int rtpMaxDistance() {
        return Math.max(SanConfigBackend.intValue(RTP_MAX_DISTANCE), rtpMinDistance() + 1);
    }

    public static int rtpNetherMinDistance() {
        return SanConfigBackend.intValue(RTP_NETHER_MIN_DISTANCE);
    }

    public static int rtpNetherMaxDistance() {
        return Math.max(SanConfigBackend.intValue(RTP_NETHER_MAX_DISTANCE), rtpNetherMinDistance() + 1);
    }

    public static int rtpEndMinDistance() {
        int value = SanConfigBackend.intValue(RTP_END_MIN_DISTANCE);
        return value < 0 ? rtpMinDistance() : value;
    }

    public static int rtpEndMaxDistance() {
        int value = SanConfigBackend.intValue(RTP_END_MAX_DISTANCE);
        return value < 0 ? rtpMaxDistance() : Math.max(value, rtpEndMinDistance() + 1);
    }

    public static int teleportRequestTimeoutSeconds() {
        return SanConfigBackend.intValue(TELEPORT_REQUEST_TIMEOUT_SECONDS);
    }

    public static boolean allowSelfTeleportRequests() {
        return SanConfigBackend.booleanValue(ALLOW_SELF_TELEPORT_REQUESTS);
    }

    public static int tpaRequestCooldownSeconds() {
        return SanConfigBackend.intValue(TPA_REQUEST_COOLDOWN_SECONDS);
    }

    public static boolean commandEnabled(String command) {
        return SanConfigBackend.commandEnabled(configCommand(command));
    }

    public static int commandPermission(String command) {
        return SanConfigBackend.commandPermission(configCommand(command));
    }

    static List<ValueDefinition> values() {
        return VALUES;
    }

    static List<String> commandComments() {
        return COMMAND_COMMENTS;
    }

    static List<CommandDefinition> commands() {
        return COMMANDS;
    }

    static boolean isValueKey(String key) {
        return VALUE_KEYS.contains(key);
    }

    static boolean isCommandName(String name) {
        return COMMAND_NAMES.contains(name);
    }

    private static ValueDefinition stringValue(
            String key,
            String defaultValue,
            Set<String> allowedValues,
            String... comments
    ) {
        return new ValueDefinition(
                key,
                ValueType.STRING,
                defaultValue,
                Integer.MIN_VALUE,
                Integer.MAX_VALUE,
                Set.copyOf(allowedValues),
                List.of(comments)
        );
    }

    private static ValueDefinition booleanValue(String key, boolean defaultValue, String... comments) {
        return new ValueDefinition(
                key,
                ValueType.BOOLEAN,
                defaultValue,
                Integer.MIN_VALUE,
                Integer.MAX_VALUE,
                Set.of(),
                List.of(comments)
        );
    }

    private static ValueDefinition intValue(String key, int defaultValue, int min, int max, String... comments) {
        return new ValueDefinition(
                key,
                ValueType.INTEGER,
                defaultValue,
                min,
                max,
                Set.of(),
                List.of(comments)
        );
    }

    private static CommandDefinition command(String name, boolean enabled, int permissionLevel) {
        return new CommandDefinition(name, enabled, permissionLevel);
    }

    private static String configCommand(String command) {
        return DERIVED_COMMANDS.getOrDefault(command, command);
    }

    private static String defaultComment(Object value) {
        return DEFAULT_VALUE_COMMENT_PREFIX + formatValue(value);
    }

    private static String formatValue(Object value) {
        return value instanceof String stringValue ? "\"" + stringValue + "\"" : value.toString();
    }

    enum ValueType {
        STRING,
        BOOLEAN,
        INTEGER
    }

    record ValueDefinition(
            String key,
            ValueType type,
            Object defaultValue,
            int min,
            int max,
            Set<String> allowedValues,
            List<String> comments
    ) {
        List<String> commentsWithDefaultValue() {
            return java.util.stream.Stream.concat(
                    comments.stream(),
                    java.util.stream.Stream.of(defaultComment(defaultValue))
            ).toList();
        }

        boolean accepts(Object value) {
            return switch (type) {
                case STRING -> value instanceof String stringValue
                        && (allowedValues.isEmpty() || allowedValues.contains(stringValue));
                case BOOLEAN -> value instanceof Boolean;
                case INTEGER -> value instanceof Integer intValue && intValue >= min && intValue <= max;
            };
        }
    }

    record CommandDefinition(String name, boolean enabled, int permissionLevel) {
        List<String> enabledComments() {
            return List.of(defaultComment(enabled));
        }

        List<String> permissionLevelComments() {
            return List.of(defaultComment(permissionLevel));
        }
    }
}
