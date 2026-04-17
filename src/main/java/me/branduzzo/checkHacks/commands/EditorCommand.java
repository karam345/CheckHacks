package me.branduzzo.checkHacks.commands;

import me.branduzzo.checkHacks.CheckHacksPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class EditorCommand implements CommandExecutor {

    private final CheckHacksPlugin plugin;

    public EditorCommand(CheckHacksPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can open the editor.");
            return true;
        }
        if (!player.hasPermission("checkhacks.editor")) {
            player.sendMessage(plugin.getMessageManager().get("no-permission"));
            return true;
        }
        if (!plugin.getConfigManager().isWebEditorEnabled()) {
            player.sendMessage(plugin.getMessageManager().get("editor-disabled"));
            return true;
        }

        String token = plugin.getDatabaseManager().saveToken(
                player.getUniqueId().toString(),
                player.getName(),
                plugin.getConfigManager().getTokenExpireMinutes());

        String host = plugin.getConfigManager().getWebHost();
        int    port = plugin.getConfigManager().getWebPort();
        String url  = "http://" + host + ":" + port + "/editor?token=" + token;

        player.sendMessage(plugin.getMessageManager().get("editor-url", Map.of("url", url)));
        return true;
    }
}