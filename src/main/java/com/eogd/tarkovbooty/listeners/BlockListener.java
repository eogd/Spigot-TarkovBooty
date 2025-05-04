package com.eogd.tarkovbooty.listeners;

import com.eogd.tarkovbooty.TarkovBooty;
import com.eogd.tarkovbooty.managers.DataManager;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;
import net.md_5.bungee.api.ChatColor;

import java.util.logging.Level;

public class BlockListener implements Listener {

    private final TarkovBooty plugin;
    private final DataManager dataManager;

    public BlockListener(TarkovBooty plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();

        if (this.dataManager == null) {
            plugin.getPluginLogger().severe("DataManager 在 BlockListener 初始化时为 null！监听器可能无法正常工作。");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (dataManager == null) return;

        Block brokenBlock = event.getBlock();
        Location blockLocation = brokenBlock.getLocation();
        Player player = event.getPlayer();

        if (dataManager.isSpecialChest(blockLocation)) {
            plugin.getPluginLogger().fine("检测到特殊战利品箱子被破坏: " + dataManager.serializeLocation(blockLocation) + " 由 " + player.getName());

            try {
                boolean removed = dataManager.removeChestLocation(blockLocation);

                if (removed) {
                    dataManager.saveChestLocations();

                    player.sendMessage(ChatColor.YELLOW + "你破坏了一个特殊的战利品箱子，它已被移除记录。");
                    plugin.getPluginLogger().info("特殊战利品箱子在 " + dataManager.serializeLocation(blockLocation) + " 被玩家 " + player.getName() + " 破坏并移除记录。");
                } else {
                    plugin.getPluginLogger().warning("尝试移除特殊箱子位置 " + dataManager.serializeLocation(blockLocation) + " 失败，尽管它被标记为特殊箱子。");
                }
            } catch (Exception e) {
                plugin.getPluginLogger().log(Level.SEVERE, "移除被破坏的特殊箱子记录时发生错误 (位置: " + dataManager.serializeLocation(blockLocation) + "):", e);
                player.sendMessage(ChatColor.RED + "处理特殊箱子破坏时发生错误，请联系管理员。");
            }
        }
    }
}
