package me.sandreamin.mods.san_essentials;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Resolves player-facing messages using the client's language with a configured fallback.
 */
public final class SanLang {
    public static final Component PREFIX = Component.literal("")
            .append(coloredLiteral("[", 0xAAAAAA))
            .append(coloredLiteral("S", 0x6C8CFF))
            .append(coloredLiteral("D", 0x4D7EF7))
            .append(coloredLiteral("] ", 0xAAAAAA));

    private static final Map<String, Map<String, String>> LANGUAGES = Map.of(
            "zh_cn", load("zh_cn"),
            "zh_tw", load("zh_tw"),
            "zh_hk", load("zh_tw"),
            "en_us", load("en_us"),
            "ru_ru", load("ru_ru"),
            "de_de", load("de_de"),
            "fr_fr", load("fr_fr"),
            "ja_jp", load("ja_jp"),
            "ko_kr", load("ko_kr")
    );

    private SanLang() {
    }

    public static MutableComponent component(ServerPlayer player, String key, Object... args) {
        return componentForLanguage(languageFor(player), key, args);
    }

    public static MutableComponent componentForLanguage(String language, String key, Object... args) {
        return Component.literal(format(language, key, args));
    }

    public static MutableComponent prefixedComponent(ServerPlayer player, String key, Object... args) {
        return prefixed(component(player, key, args));
    }

    public static MutableComponent prefixed(Component component) {
        return SanConfig.messagePrefixEnabled() ? PREFIX.copy().append(component) : component.copy();
    }

    public static String languageFor(ServerPlayer player) {
        String clientLanguage = player.clientInformation().language();
        if (LANGUAGES.containsKey(clientLanguage)) {
            return clientLanguage;
        }

        String configuredLanguage = SanConfig.fallbackLanguage();
        if (LANGUAGES.containsKey(configuredLanguage)) {
            return configuredLanguage;
        }

        return "en_us";
    }

    private static String format(String language, String key, Object... args) {
        String template = LANGUAGES.getOrDefault(language, LANGUAGES.get("en_us")).get(key);
        if (template == null) {
            template = LANGUAGES.get("en_us").getOrDefault(key, key);
        }

        Object[] plainArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            plainArgs[i] = toPlainText(args[i]);
        }

        try {
            return String.format(Locale.ROOT, template, plainArgs);
        } catch (RuntimeException ignored) {
            return template;
        }
    }

    private static Object toPlainText(Object arg) {
        return arg instanceof Component component ? component.getString() : arg;
    }

    private static MutableComponent coloredLiteral(String text, int color) {
        return Component.literal(text).withStyle(style -> style.withColor(color));
    }

    private static Map<String, String> load(String language) {
        String path = "/assets/" + SanEssentials.MODID + "/lang/" + language + ".json";
        try (InputStream stream = SanLang.class.getResourceAsStream(path)) {
            if (stream == null) {
                SanEssentials.LOGGER.warn("Missing San Essentials language file: {}", path);
                return Map.of();
            }

            JsonObject json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, String> values = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                values.put(entry.getKey(), entry.getValue().getAsString());
            }
            return values;
        } catch (Exception exception) {
            SanEssentials.LOGGER.warn("Failed to load San Essentials language file: {}", path, exception);
            return Map.of();
        }
    }
}
