package com.eogd.tarkovbooty.listeners;

import com.eogd.tarkovbooty.TarkovBooty;
import com.eogd.tarkovbooty.managers.DataManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;
import net.md_5.bungee.api.ChatColor;
import java.util.logging.Level;

public class InventoryListener implements Listener {

    private final TarkovBooty plugin;
    private final DataManager dataManager;

    public InventoryListener(TarkovBooty plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();

        if (this.dataManager == null) {
            plugin.getPluginLogger().severe("DataManager 在 InventoryListener 初始化时为 null！监听器可能无法正常工作。");
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (dataManager == null) return;

        InventoryView view = event.getView();
        Inventory clickedInventory = event.getClickedInventory();
        Player player = (Player) event.getWhoClicked();

        if (view.getTitle().equals(DataManager.getGuiTitle())) {
            plugin.getPluginLogger().finest("玩家 " + player.getName() + " 在模板 GUI 中点击。槽位: " + event.getSlot() + ", 类型: " + event.getClick() + ", 动作: " + event.getAction());
            if (clickedInventory != null && clickedInventory.equals(view.getTopInventory())) {
                plugin.getPluginLogger().finest(" -> 允许在 GUI 内部的操作。");
            }

            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY &&
                    clickedInventory != null && clickedInventory.equals(view.getBottomInventory()))
            {
                plugin.getPluginLogger().finest(" -> 允许 Shift-Click 将物品从玩家背包移入 GUI。");
                return;
            }


            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY &&
                    clickedInventory != null && clickedInventory.equals(view.getTopInventory()))
            {
                plugin.getPluginLogger().fine(" -> 阻止 Shift-Click 将物品从 GUI 移出到玩家背包。");
                event.setCancelled(true);
                player.updateInventory();
                return;
            }

            if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR &&
                    clickedInventory != null && clickedInventory.equals(view.getTopInventory()))
            {
                plugin.getPluginLogger().fine(" -> 阻止双击收集 GUI 内物品到鼠标。");
                event.setCancelled(true);
                player.updateInventory();
                return;
            }

            if (event.getAction() == InventoryAction.HOTBAR_SWAP || event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD) {
                if (clickedInventory != null && clickedInventory.equals(view.getTopInventory())) {
                    plugin.getPluginLogger().fine(" -> 阻止使用数字键将物品从 GUI 移到玩家热键栏。");
                    event.setCancelled(true);
                    player.updateInventory();
                    return;
                }
            }

            if (clickedInventory != null && clickedInventory.equals(view.getBottomInventory())) {
                plugin.getPluginLogger().finest(" -> 允许在玩家背包内的操作。");
            }

        }
    }


    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {

        if (dataManager == null) return;

        InventoryView view = event.getView();
        Player player = (Player) event.getPlayer();
        Inventory closedInventory = view.getTopInventory();

        if (view.getTitle().equals(DataManager.getGuiTitle())) {
            plugin.getPluginLogger().info("玩家 " + player.getName() + " 关闭了战利品模板 GUI，正在保存...");

            try {
                dataManager.saveGuiData(closedInventory);
                player.sendMessage(ChatColor.GREEN + "战利品模板已保存。");
            } catch (Exception e) {
                plugin.getPluginLogger().log(Level.SEVERE, "保存 GUI 数据时发生错误 (玩家: " + player.getName() + "):", e);
                player.sendMessage(ChatColor.RED + "保存战利品模板时出错！请联系管理员。");
            }
        }
    }
}