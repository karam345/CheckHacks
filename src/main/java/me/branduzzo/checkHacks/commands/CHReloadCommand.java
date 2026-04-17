package me.branduzzo.checkHacks.commands;

import me.branduzzo.checkHacks.CheckHacksPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Map;

public class CHReloadCommand implements CommandExecutor {

    private final CheckHacksPlugin plugin;

    public CHReloadCommand(CheckHacksPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("checkhacks.reload")) {
            sender.sendMessage(plugin.getMessageManager().get("no-permission"));
            return true;
        }
        plugin.getConfigManager().reload();
        plugin.getMessageManager().load();
        sender.sendMessage(plugin.getMessageManager().get("reload-done"));
        return true;
    }
}