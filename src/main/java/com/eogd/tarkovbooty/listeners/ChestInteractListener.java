package com.eogd.tarkovbooty.listeners;

import com.eogd.tarkovbooty.TarkovBooty;
import com.eogd.tarkovbooty.managers.ConfigManager;
import com.eogd.tarkovbooty.managers.DataManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class ChestInteractListener implements Listener {

    private final TarkovBooty plugin;
    private final DataManager dataManager;
    private final ConfigManager configManager;
    private final Map<UUID, OpeningData> openingAttempts = new HashMap<>();
    private static class OpeningData {
        final BukkitTask task;
        final Location initialLocation;

        OpeningData(BukkitTask task, Location initialLocation) {
            this.task = task;
            this.initialLocation = initialLocation;
        }
    }

    public ChestInteractListener(TarkovBooty plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.configManager = plugin.getConfigManager();

        if (dataManager == null || configManager == null) {
            plugin.getPluginLogger().severe("DataManager 或 ConfigManager 在 ChestInteractListener 初始化时为 null！");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {

        if (!configManager.isOpeningDelayEnabled()) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }

        if (dataManager.isSpecialChest(clickedBlock.getLocation())) {
            if (clickedBlock.getState() instanceof Chest) {
                Player player = event.getPlayer();
                UUID playerUUID = player.getUniqueId();
                Chest chest = (Chest) clickedBlock.getState();

                plugin.getPluginLogger().fine("玩家 " + player.getName() + " 右键点击了特殊箱子: " + dataManager.serializeLocation(clickedBlock.getLocation()));

                event.setCancelled(true);

                if (openingAttempts.containsKey(playerUUID)) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getOpeningAlreadyOpeningMessage()));
                    plugin.getPluginLogger().fine(" -> 玩家已在尝试开启，操作中止。");
                    return;
                }

                int delaySeconds = configManager.getOpeningDelaySeconds();
                long delayTicks = delaySeconds * 20L;

                player.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getOpeningStartMessage()));
                plugin.getPluginLogger().fine(" -> 开始 " + delaySeconds + " 秒开启延迟...");

                Location initialBlockLocation = player.getLocation().getBlock().getLocation();

                BukkitTask task = new BukkitRunnable() {
                    @Override
                    public void run() {
                        openingAttempts.remove(playerUUID);

                        if (!player.isOnline()) {
                            plugin.getPluginLogger().fine(" -> 开启任务执行时玩家 " + player.getName() + " 已离线。");
                            return;
                        }
                        Block currentBlock = clickedBlock.getLocation().getBlock();
                        if (!(currentBlock.getState() instanceof Chest) || !dataManager.isSpecialChest(currentBlock.getLocation())) {
                            plugin.getPluginLogger().fine(" -> 开启任务执行时箱子 " + dataManager.serializeLocation(clickedBlock.getLocation()) + " 不再有效。");
                            player.sendMessage(ChatColor.RED + "箱子在你开启时消失了！");
                            return;
                        }

                        plugin.getPluginLogger().fine(" -> 玩家 " + player.getName() + " 成功完成开启延迟，强制打开箱子 " + dataManager.serializeLocation(clickedBlock.getLocation()));

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (player.isOnline()) {

                                    Inventory chestInventory = ((Chest) currentBlock.getState()).getInventory();
                                    player.openInventory(chestInventory);

                                }
                            }
                        }.runTask(plugin);
                    }
                }.runTaskLater(plugin, delayTicks);

                openingAttempts.put(playerUUID, new OpeningData(task, initialBlockLocation));
            } else {
                plugin.getPluginLogger().warning("特殊箱子位置 " + dataManager.serializeLocation(clickedBlock.getLocation()) + " 的方块类型不再是 Chest (" + clickedBlock.getType() + ")，交互被忽略。");

            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (openingAttempts.containsKey(playerUUID)) {
            OpeningData data = openingAttempts.get(playerUUID);
            Location from = data.initialLocation;
            Location to = event.getTo();

            if (to == null || from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
                plugin.getPluginLogger().fine("玩家 " + player.getName() + " 在开启箱子时移动，取消操作。");

                data.task.cancel();

                openingAttempts.remove(playerUUID);

                player.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getOpeningCancelMoveMessage()));
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (openingAttempts.containsKey(playerUUID)) {
            plugin.getPluginLogger().fine("玩家 " + player.getName() + " 在尝试开启箱子时退出，取消操作。");
            OpeningData data = openingAttempts.remove(playerUUID);
            data.task.cancel();
        }
    }

    public void cancelAllOpeningTasks() {
        if (!openingAttempts.isEmpty()) {
            plugin.getPluginLogger().info("正在取消 " + openingAttempts.size() + " 个待处理的箱子开启任务...");
            for (Map.Entry<UUID, OpeningData> entry : openingAttempts.entrySet()) {
                entry.getValue().task.cancel();
                Player player = plugin.getServer().getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    player.sendMessage(ChatColor.RED + "服务器关闭/重载，你的箱子开启操作已被取消。");
                }
            }
            openingAttempts.clear();
            plugin.getPluginLogger().info("所有待处理的箱子开启任务已取消。");
        }
    }
}