package com.eogd.tarkovbooty.commands;

import com.eogd.tarkovbooty.TarkovBooty;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginDescriptionFile;
import org.jetbrains.annotations.NotNull;

public class TInfoCommand implements CommandExecutor {

    private final TarkovBooty plugin;
    private final String authorInfo = "GitHub: eogd";
    private final String descriptionInfo = "为你的服务器引入高风险高回报的摸金战利品搜刮体验！此插件添加了特殊的战利品箱子，它们会定期刷新内含宝物的物品。通过 GUI 配置战利品池，设置刷新间隔和倒计时广播，并通过开启延迟机制（右键按住开启，移动则取消）让玩家为他们的奖励付出努力。很适合摸金类伺服器！。";

    public TInfoCommand(TarkovBooty plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("tarkovbooty.info")) {
            String noPermMessage = command.getPermissionMessage();
            sender.sendMessage(ChatColor.RED + (noPermMessage != null ? noPermMessage : "你没有权限使用此命令。"));
            return true;
        }

        if (args.length > 0) {
            sender.sendMessage(ChatColor.RED + "用法: /" + label);
            return false;
        }

        PluginDescriptionFile pluginDescription = plugin.getDescription();
        String pluginName = pluginDescription.getName();
        String pluginVersion = pluginDescription.getVersion();

        sender.sendMessage(ChatColor.GOLD + "--- 插件信息: " + ChatColor.AQUA + pluginName + ChatColor.GOLD + " ---");
        sender.sendMessage(ChatColor.GRAY + "版本: " + ChatColor.WHITE + pluginVersion);
        sender.sendMessage(ChatColor.GRAY + "作者:" + ChatColor.WHITE + authorInfo);
        sender.sendMessage(ChatColor.GRAY + "描述: " + ChatColor.WHITE + descriptionInfo);
        sender.sendMessage(ChatColor.GOLD + "----------------------------------");

        return true;
    }
}