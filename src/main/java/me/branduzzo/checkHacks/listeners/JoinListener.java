package me.branduzzo.checkHacks.listeners;

import me.branduzzo.checkHacks.CheckHacksPlugin;
import me.branduzzo.checkHacks.ClientType;
import me.branduzzo.checkHacks.HackDefinition;
import me.branduzzo.checkHacks.utils.SchedulerUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class JoinListener implements Listener {

    private final CheckHacksPlugin plugin;
    private final Set<UUID> alreadyHackChecked = ConcurrentHashMap.newKeySet();
    private final Set<UUID> alreadyLangChecked = ConcurrentHashMap.newKeySet();

    public JoinListener(CheckHacksPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        SchedulerUtil.runForEntityLater(plugin, player, () -> {
            if (!player.isOnline()) return;
            Set<String> channels = player.getListeningPluginChannels();
            ClientType type = detectClientType(channels);
            plugin.getClientDataManager().setClientType(uuid, type);
            plugin.getLogger().info("[CheckHacks] " + player.getName() + " client type: " + type
                    + " channels=" + channels);
        }, 5L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (plugin.getConfigManager().isJoinCheckEnabled()) {
            if (!plugin.getConfigManager().isOnlyFirstJoin() || alreadyHackChecked.add(uuid)) {
                plugin.getLogger().info("[CheckHacks] Queued auto-join check for " + player.getName() + ".");
                SchedulerUtil.runForEntityLater(plugin, player, () -> {
                    if (!player.isOnline()) return;
                    java.util.List<HackDefinition> hacks = plugin.getConfigManager().getJoinCheckHacks();
                    if (hacks.isEmpty()) {
                        plugin.getLogger().warning("[CheckHacks] Auto-join check skipped for "
                                + player.getName() + ": no hacks configured.");
                        return;
                    }
                    plugin.getMessageManager().broadcastAlerts(
                            plugin.getMessageManager().get("join-check", Map.of("player", player.getName())));
                    plugin.getCheckManager().startCheck(player, null, hacks, true, "Auto-join check");
                }, 60L);
            } else {
                plugin.getLogger().info("[CheckHacks] Auto-join check skipped for " + player.getName()
                        + ": only-first-join is enabled and this player was already checked.");
            }
        } else {
            plugin.getLogger().info("[CheckHacks] Auto-join check disabled in checkhacks.yml.");
        }

        if (plugin.getConfigManager().isLangJoinCheckEnabled()) {
            boolean isFirst = alreadyLangChecked.add(uuid);
            if (!plugin.getConfigManager().isLangOnlyFirstJoin() || isFirst) {
                SchedulerUtil.runForEntityLater(plugin, player, () -> {
                    if (!player.isOnline()) return;
                    Map<String, String> langs = plugin.getConfigManager().getLanguages();
                    if (langs.isEmpty()) return;
                    plugin.getMessageManager().broadcastAlerts(
                            plugin.getMessageManager().get("lang-join-check", Map.of("player", player.getName())));
                    plugin.getLangCheckManager().startCheck(player, null, langs);
                }, 80L);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        plugin.getCheckManager().abortCheck(uuid);
        plugin.getLangCheckManager().abortCheck(uuid);
        plugin.getClientDataManager().remove(uuid);
    }

    private ClientType detectClientType(Set<String> channels) {
        for (String ch : channels) {
            String lower = ch.toLowerCase();
            if (lower.startsWith("fabric") || lower.contains("fabric-api")
                    || lower.contains("fabric-networking") || lower.contains("fabric-screen")) {
                return ClientType.FABRIC;
            }
        }
        for (String ch : channels) {
            String lower = ch.toLowerCase();
            if (lower.startsWith("fml") || lower.startsWith("forge")
                    || lower.contains("forge:") || lower.contains("fml:")) {
                return ClientType.FORGE;
            }
        }
        if (channels.isEmpty()) return ClientType.VANILLA;
        return ClientType.UNKNOWN;
    }
}
