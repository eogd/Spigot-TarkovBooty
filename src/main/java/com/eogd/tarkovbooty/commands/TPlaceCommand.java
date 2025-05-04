package com.eogd.tarkovbooty.commands;

import com.eogd.tarkovbooty.TarkovBooty;
import com.eogd.tarkovbooty.managers.DataManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import net.md_5.bungee.api.ChatColor;

import java.util.logging.Level;

public class TPlaceCommand implements CommandExecutor {

    private final TarkovBooty plugin;
    private final DataManager dataManager;
    public TPlaceCommand(TarkovBooty plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();

        if (this.dataManager == null) {
            plugin.getPluginLogger().severe("DataManager 在 TPlaceCommand 初始化时为 null！放置指令可能无法工作。");
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "这个指令只能由玩家执行。");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("tarkovbooty.admin")) {
            String noPermMessage = command.getPermissionMessage();
            player.sendMessage(ChatColor.RED + (noPermMessage != null ? noPermMessage : "你没有权限执行此指令。"));
            return true;
        }

        if (args.length > 0) {
            player.sendMessage(ChatColor.RED + "用法错误: /" + label);
            return false;
        }

        if (dataManager == null) {
            plugin.getPluginLogger().severe("DataManager 在执行 /tplace 时为 null！无法放置箱子。");
            player.sendMessage(ChatColor.RED + "插件内部错误，无法放置箱子。请联系管理员。");
            return true;
        }

        Location playerLocation = player.getLocation();
        Location blockLocation = playerLocation.getBlock().getLocation();
        Block targetBlock = blockLocation.getBlock();

        if (dataManager.isSpecialChest(blockLocation)) {
            player.sendMessage(ChatColor.YELLOW + "这里已经是一个特殊的战利品箱子了。");
            return true;
        }

        Material targetType = targetBlock.getType();
        if (!targetType.isAir() && !targetType.isSolid() && targetType != Material.WATER && targetType != Material.LAVA ) {
            player.sendMessage(ChatColor.RED + "无法在此处放置箱子 (方块类型: " + targetType.name() + ")。请选择一个合适的空地或可替换的方块。");
            return true;
        }
        if (targetType == Material.CHEST || targetType == Material.TRAPPED_CHEST) {
            player.sendMessage(ChatColor.YELLOW + "注意：你正在将一个普通箱子标记为特殊的战利品箱子。");
        }


        try {
            targetBlock.setType(Material.CHEST, true);

            if (targetBlock.getType() == Material.CHEST) {
                boolean added = dataManager.addChestLocation(blockLocation);

                if (added) {
                    dataManager.saveChestLocations();
                    player.sendMessage(ChatColor.BLACK + "特殊的战利品箱子已成功放置并记录!");
                    plugin.getPluginLogger().info("玩家 " + player.getName() + " 在 " + dataManager.serializeLocation(blockLocation) + " 放置了一个特殊箱子。");
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "无法记录箱子位置，可能已经存在记录。");
                    return true;
                }
            } else {
                player.sendMessage(ChatColor.RED + "无法将目标方块设置为箱子。请检查服务器日志。");
                plugin.getPluginLogger().warning("尝试在 " + dataManager.serializeLocation(blockLocation) + " 放置箱子，但设置方块类型后检查失败 (当前类型: " + targetBlock.getType() + ")。");
                return true;
            }
        } catch (Exception e) {
            plugin.getPluginLogger().log(Level.SEVERE, "放置特殊箱子时发生错误:", e);
            player.sendMessage(ChatColor.RED + "放置箱子时发生未知错误。请查看服务器日志。");
            return true;
        }
    }
}
