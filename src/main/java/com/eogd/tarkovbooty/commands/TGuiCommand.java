package com.eogd.tarkovbooty.commands;

import com.eogd.tarkovbooty.TarkovBooty;
import com.eogd.tarkovbooty.managers.DataManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import net.md_5.bungee.api.ChatColor;
import java.util.logging.Level;
public class TGuiCommand implements CommandExecutor {

    private final TarkovBooty plugin;
    private final DataManager dataManager;
    public TGuiCommand(TarkovBooty plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();

        if (this.dataManager == null) {
            plugin.getPluginLogger().severe("DataManager 在 TGuiCommand 初始化时为 null！GUI 指令可能无法工作。");
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "这个指令只能由玩家执行。");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("tarkovbooty.gui")) {
            String noPermMessage = command.getPermissionMessage();
            player.sendMessage(ChatColor.RED + (noPermMessage != null ? noPermMessage : "你没有权限执行此指令。"));
            return true;
        }

        if (args.length > 0) {
            player.sendMessage(ChatColor.RED + "用法错误: /" + label);
            return false;
        }

        if (dataManager == null) {
            plugin.getPluginLogger().severe("DataManager 在执行 /tgui 时为 null！无法打开 GUI。");
            player.sendMessage(ChatColor.RED + "插件内部错误，无法打开 GUI。请联系管理员。");
            return true;
        }

        try {
            Inventory guiInventory = dataManager.getGuiTemplateInventory();
            if (guiInventory != null) {
                player.openInventory(guiInventory);
                plugin.getPluginLogger().fine("玩家 " + player.getName() + " 打开了战利品模板 GUI。");
                return true;
            } else {
                plugin.getPluginLogger().severe("DataManager.getGuiTemplateInventory() 返回了 null！无法打开 GUI。");
                player.sendMessage(ChatColor.RED + "插件内部错误，无法获取 GUI 数据。请联系管理员。");
                return true;
            }
        } catch (Exception e) {
            plugin.getPluginLogger().log(Level.SEVERE, "为玩家 " + player.getName() + " 打开 GUI 时发生错误:", e);
            player.sendMessage(ChatColor.RED + "打开 GUI 时发生未知错误。请查看服务器日志。");
            return true;
        }
    }
}