package com.eogd.tarkovbooty.commands;

import com.eogd.tarkovbooty.TarkovBooty;
import com.eogd.tarkovbooty.managers.ConfigManager;
import com.eogd.tarkovbooty.managers.DataManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import net.md_5.bungee.api.ChatColor;

import java.util.logging.Level;

public class TReloadCommand implements CommandExecutor {

    private final TarkovBooty plugin;
    private final ConfigManager configManager;
    private final DataManager dataManager;

    public TReloadCommand(TarkovBooty plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.dataManager = plugin.getDataManager();

        if (this.configManager == null || this.dataManager == null) {
            plugin.getPluginLogger().severe("ConfigManager 或 DataManager 在 TReloadCommand 初始化时为 null！重载指令可能无法工作。");
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("tarkovbooty.admin")) {
            String noPermMessage = command.getPermissionMessage();
            String message = ChatColor.RED + (noPermMessage != null && sender instanceof Player ? noPermMessage : "你没有权限执行此指令。");
            sender.sendMessage(message);
            return true;
        }

        if (args.length > 0) {
            sender.sendMessage(ChatColor.RED + "用法错误: /" + label);
            return false;
        }

        if (configManager == null || dataManager == null) {
            plugin.getPluginLogger().severe("ConfigManager 或 DataManager 在执行 /treload 时为 null！无法重载配置。");
            sender.sendMessage(ChatColor.RED + "插件内部错误，无法重载配置。请联系管理员。");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "开始重载 TarkovBooty 插件配置...");
        long startTime = System.currentTimeMillis();

        try {
            configManager.reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "- config.yml 已重新加载。");

            dataManager.loadGuiData();
            sender.sendMessage(ChatColor.GREEN + "- gui_data.yml 已重新加载。");

            dataManager.loadChestLocations();
            sender.sendMessage(ChatColor.GREEN + "- chests.yml 已重新加载。");

            long duration = System.currentTimeMillis() - startTime;
            sender.sendMessage(ChatColor.GREEN + "TarkovBooty 配置重载成功! (耗时 " + duration + "ms)");
            plugin.getPluginLogger().info("插件配置已由 " + sender.getName() + " 重新加载 (耗时 " + duration + "ms)。");

            return true;

        } catch (Exception e) {
            plugin.getPluginLogger().log(Level.SEVERE, "重载插件配置时发生错误:", e);
            sender.sendMessage(ChatColor.RED + "重载配置时发生错误！请检查服务器日志获取详细信息。");
            return true;
        }
    }
}
