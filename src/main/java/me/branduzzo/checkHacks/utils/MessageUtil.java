package me.branduzzo.checkHacks.utils;

import me.branduzzo.checkHacks.CheckHacksPlugin;
import net.kyori.adventure.text.Component;

import java.util.Map;

public class MessageUtil {

    public static Component parse(CheckHacksPlugin plugin, String key, Map<String, String> placeholders) {
        return plugin.getMessageManager().get(key, placeholders);
    }

    public static Component parseRaw(CheckHacksPlugin plugin, String raw, Map<String, String> placeholders) {
        String prefix = plugin.getConfigManager().getPrefix();
        raw = raw.replace("{prefix}", prefix);
        for (Map.Entry<String, String> e : placeholders.entrySet())
            raw = raw.replace("{" + e.getKey() + "}", e.getValue());
        return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(raw);
    }

    public static void broadcastAlerts(CheckHacksPlugin plugin, Component msg) {
        plugin.getMessageManager().broadcastAlerts(msg);
    }

    public static void broadcast(CheckHacksPlugin plugin, String key,
                                 String ignored, Map<String, String> placeholders) {
        plugin.getMessageManager().broadcastAlerts(plugin.getMessageManager().get(key, placeholders));
    }
}