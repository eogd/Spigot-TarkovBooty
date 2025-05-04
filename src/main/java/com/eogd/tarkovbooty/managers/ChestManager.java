package com.eogd.tarkovbooty.managers;

import com.eogd.tarkovbooty.TarkovBooty;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class ChestManager {

    private final TarkovBooty plugin;
    private final DataManager dataManager;
    private final ConfigManager configManager;
    private final Random random = new Random();
    private BukkitTask refreshTask;
    private BukkitTask countdownTask;
    private long nextRefreshTimeMillis = -1;

    private static final int SLOT_SELECTION_CHANCE_PERCENT = 5;
    private static final Pattern QUANTITY_PATTERN = Pattern.compile("(?:数量|Quantity):\\s*(\\d+)\\s*-\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PROBABILITY_PATTERN = Pattern.compile("(?:概率|Probability):\\s*(\\d{1,3})\\s*%", Pattern.CASE_INSENSITIVE);


    public ChestManager(TarkovBooty plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.configManager = plugin.getConfigManager();

        if (this.dataManager == null || this.configManager == null) {
            plugin.getPluginLogger().log(Level.SEVERE, "DataManager 或 ConfigManager 未初始化! ChestManager 可能无法正常工作。");
        }
    }


    public void startRefreshTask() {
        stopRefreshTask();

        long intervalSeconds = configManager.getChestRefreshInterval();
        if (intervalSeconds <= 0) {
            plugin.getPluginLogger().warning("箱子刷新间隔无效 (" + intervalSeconds + " 秒)，刷新和倒计时任务未启动。");
            return;
        }

        long intervalTicks = intervalSeconds * 20L;
        long initialDelayTicks = 20L * 10;

        plugin.getPluginLogger().info("正在启动箱子刷新任务，间隔: " + intervalSeconds + " 秒 (" + intervalTicks + " ticks)...");

        refreshTask = new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getPluginLogger().fine("执行箱子刷新任务...");
                refreshAllSpecialChests();

                nextRefreshTimeMillis = System.currentTimeMillis() + intervalTicks * 50;
                plugin.getPluginLogger().finest("下一次刷新时间戳更新为: " + nextRefreshTimeMillis);
            }
        }.runTaskTimer(plugin, initialDelayTicks, intervalTicks);

        nextRefreshTimeMillis = System.currentTimeMillis() + initialDelayTicks * 50;
        plugin.getPluginLogger().info("首次箱子刷新计划在: " + new java.util.Date(nextRefreshTimeMillis));

        if (configManager.isBroadcastCountdownEnabled()) {
            startCountdownTask();
        } else {
            plugin.getPluginLogger().info("倒计时广播已禁用。");
        }

        plugin.getPluginLogger().info("箱子刷新和相关任务已启动。");
    }

    private void startCountdownTask() {
        if (countdownTask != null && !countdownTask.isCancelled()) {
            countdownTask.cancel();
        }

        plugin.getPluginLogger().info("正在启动刷新倒计时任务...");
        countdownTask = new BukkitRunnable() {
            private int lastAnnouncedSecond = -1;

            @Override
            public void run() {
                if (nextRefreshTimeMillis <= 0) {
                    return;
                }

                long currentTime = System.currentTimeMillis();
                long remainingMillis = nextRefreshTimeMillis - currentTime;
                long remainingSeconds = (long) Math.ceil(remainingMillis / 1000.0);

                if (remainingSeconds >= 1 && remainingSeconds <= 10) {
                    if (remainingSeconds != lastAnnouncedSecond) {
                        lastAnnouncedSecond = (int) remainingSeconds;
                        String messageFormat = configManager.getCountdownMessageFormat();
                        String message = ChatColor.translateAlternateColorCodes('&',
                                messageFormat.replace("{seconds}", String.valueOf(remainingSeconds)));
                        Bukkit.broadcastMessage(message);
                        plugin.getPluginLogger().fine("广播倒计时: " + remainingSeconds + " 秒");
                    }
                } else {
                    lastAnnouncedSecond = -1;
                }
                if (remainingMillis < -1000) {
                    plugin.getPluginLogger().fine("检测到刷新时间已过，等待主任务更新下次刷新时间。");
                    nextRefreshTimeMillis = -1;
                    lastAnnouncedSecond = -1;
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
        plugin.getPluginLogger().info("刷新倒计时任务已启动。");
    }

    public void stopRefreshTask() {
        if (refreshTask != null && !refreshTask.isCancelled()) {
            plugin.getPluginLogger().info("正在停止箱子刷新主任务...");
            refreshTask.cancel();
            refreshTask = null;
            plugin.getPluginLogger().info("箱子刷新主任务已停止。");
        } else {
            plugin.getPluginLogger().fine("箱子刷新主任务未运行或已被取消。");
        }

        if (countdownTask != null && !countdownTask.isCancelled()) {
            plugin.getPluginLogger().info("正在停止刷新倒计时任务...");
            countdownTask.cancel();
            countdownTask = null;
            plugin.getPluginLogger().info("刷新倒计时任务已停止。");
        } else {
            plugin.getPluginLogger().fine("刷新倒计时任务未运行或已被取消。");
        }

        nextRefreshTimeMillis = -1;
    }

    private void refreshAllSpecialChests() {
        Set<Location> chestLocations = dataManager.getSpecialChestLocations();
        Inventory templateInventory = dataManager.getGuiTemplateInventory();

        if (templateInventory == null || isInventoryEmpty(templateInventory)) {
            plugin.getPluginLogger().fine("战利品模板为空或无效，跳过箱子刷新。");
            return;
        }

        if (chestLocations.isEmpty()) {
            plugin.getPluginLogger().fine("没有找到特殊箱子位置，跳过刷新。");
            return;
        }

        plugin.getPluginLogger().fine("开始刷新 " + chestLocations.size() + " 个特殊箱子...");
        int refreshedCount = 0;
        int failedCount = 0;

        for (Location loc : chestLocations) {
            if (!loc.isWorldLoaded() || !loc.getChunk().isLoaded()) {
                plugin.getPluginLogger().finer("箱子位置 " + dataManager.serializeLocation(loc) + " 所在区块未加载，跳过刷新。");
                failedCount++;
                continue;
            }

            Block block = loc.getBlock();
            if (block.getState() instanceof Chest) {
                Chest chest = (Chest) block.getState();
                Inventory chestInventory = chest.getInventory();

                if (chestInventory.getHolder() instanceof DoubleChest) {
                    DoubleChest doubleChest = (DoubleChest) chestInventory.getHolder();
                    chestInventory = doubleChest.getInventory();
                }

                plugin.getPluginLogger().finer("正在刷新箱子: " + dataManager.serializeLocation(loc));
                chestInventory.clear();
                List<ItemStack> newLoot = generateLootForChest(templateInventory);

                if (newLoot.isEmpty() && !isInventoryEmpty(templateInventory)) {
                    plugin.getPluginLogger().fine(" -> 箱子 " + dataManager.serializeLocation(loc) + " 概率选择为空，执行保底机制...");
                    ItemStack guaranteedItem = generateGuaranteedLootItem(templateInventory);
                    if (guaranteedItem != null) {
                        newLoot.add(guaranteedItem);
                        plugin.getPluginLogger().fine("  -> 保底机制选择了物品: " + guaranteedItem.getType() + " x " + guaranteedItem.getAmount());
                    } else {
                        plugin.getPluginLogger().warning("  -> 保底机制无法选择物品，模板可能意外变空？");
                    }
                }

                populateChest(chestInventory, newLoot);
                refreshedCount++;
            } else {
                plugin.getPluginLogger().warning("位置 " + dataManager.serializeLocation(loc) + " 的方块不再是箱子 ("+ block.getType() +")，将从记录中移除。");
                dataManager.removeChestLocation(loc);
                dataManager.saveChestLocations();
                failedCount++;
            }
        }

        if (refreshedCount > 0 || failedCount > 0) {
            plugin.getPluginLogger().fine("箱子刷新完成: " + refreshedCount + " 个成功, " + failedCount + " 个失败/跳过/移除。");
        }
    }

    private List<ItemStack> generateLootForChest(@NotNull Inventory templateInventory) {
        List<ItemStack> generatedLoot = new ArrayList<>();
        ItemStack[] templateContents = templateInventory.getContents();
        plugin.getPluginLogger().finest(" -> 开始为当前箱子生成战利品 (5% 概率)...");
        for (ItemStack templateItem : templateContents) {
            if (templateItem != null && templateItem.getType() != Material.AIR) {
                plugin.getPluginLogger().finest("  -> 检查模板槽位物品: " + templateItem.getType());
                if (random.nextInt(100) < SLOT_SELECTION_CHANCE_PERCENT) {
                    plugin.getPluginLogger().finest("   -> 槽位通过 " + SLOT_SELECTION_CHANCE_PERCENT + "% 概率检查！");
                    ItemStack lootItem = processItemQuantity(templateItem);
                    if (lootItem != null) {
                        generatedLoot.add(lootItem);
                        plugin.getPluginLogger().finest("    -> 生成物品: " + lootItem.getType() + " x " + lootItem.getAmount());
                    }
                } else {
                    plugin.getPluginLogger().finest("   -> 槽位未通过 " + SLOT_SELECTION_CHANCE_PERCENT + "% 概率检查，跳过。");
                }
            }
        }
        plugin.getPluginLogger().finest(" -> 当前箱子战利品生成完毕 (5% 概率)，共 " + generatedLoot.size() + " 堆物品。");
        return generatedLoot;
    }

    @Nullable
    private ItemStack generateGuaranteedLootItem(@NotNull Inventory templateInventory) {
        List<ItemStack> validTemplateItems = Arrays.stream(templateInventory.getContents())
                .filter(Objects::nonNull)
                .filter(item -> item.getType() != Material.AIR)
                .collect(Collectors.toList());
        if (validTemplateItems.isEmpty()) {
            return null;
        }
        ItemStack chosenTemplateItem = validTemplateItems.get(random.nextInt(validTemplateItems.size()));
        plugin.getPluginLogger().finest("  -> 保底机制随机选中模板物品: " + chosenTemplateItem.getType());
        return processItemQuantity(chosenTemplateItem);
    }

    @Nullable
    private ItemStack processItemQuantity(@NotNull ItemStack templateItem) {
        if (templateItem == null || templateItem.getType() == Material.AIR) {
            return null;
        }
        ItemStack lootItem = templateItem.clone();
        ItemMeta meta = lootItem.getItemMeta();
        int minQuantity = lootItem.getAmount();
        int maxQuantity = lootItem.getAmount();
        if (meta != null && meta.hasLore()) {
            List<String> lore = meta.getLore();
            if (lore != null) {
                for (String line : lore) {
                    Matcher quantMatcher = QUANTITY_PATTERN.matcher(line);
                    if (quantMatcher.find()) {
                        try {
                            int q1 = Integer.parseInt(quantMatcher.group(1));
                            int q2 = Integer.parseInt(quantMatcher.group(2));
                            minQuantity = Math.max(1, Math.min(q1, q2));
                            maxQuantity = Math.max(minQuantity, Math.max(q1, q2));
                            maxQuantity = Math.min(maxQuantity, lootItem.getMaxStackSize());
                            minQuantity = Math.min(minQuantity, maxQuantity);
                            plugin.getPluginLogger().finest("    - 发现数量范围: " + minQuantity + "-" + maxQuantity);
                            break;
                        } catch (NumberFormatException e) {
                            plugin.getPluginLogger().warning("    - 解析物品 " + lootItem.getType() + " 的数量范围时出错: " + line);
                        }
                    }
                }
            }
        } else {
            plugin.getPluginLogger().finest("    - 未找到 Lore 或数量范围，使用默认数量: " + minQuantity);
        }
        int finalQuantity = (minQuantity == maxQuantity) ? minQuantity : random.nextInt(maxQuantity - minQuantity + 1) + minQuantity;
        lootItem.setAmount(finalQuantity);
        return lootItem;
    }

    private void populateChest(@NotNull Inventory chestInventory, @NotNull List<ItemStack> lootItems) {
        if (lootItems.isEmpty()) {
            plugin.getPluginLogger().finer("  -> 没有生成战利品 (包括保底后)，箱子保持为空。");
            return;
        }
        List<Integer> emptySlots = new ArrayList<>();
        for (int i = 0; i < chestInventory.getSize(); i++) {
            if (chestInventory.getItem(i) == null || chestInventory.getItem(i).getType() == Material.AIR) {
                emptySlots.add(i);
            }
        }
        if (emptySlots.isEmpty()) {
            plugin.getPluginLogger().warning("  -> 箱子已满，无法放入生成的战利品。");
            return;
        }
        Collections.shuffle(emptySlots, random);
        int itemsPlaced = 0;
        for (ItemStack item : lootItems) {
            if (emptySlots.isEmpty()) {
                plugin.getPluginLogger().warning("  -> 箱子空间不足，部分战利品未能放入: " + item.getType() + " x " + item.getAmount());
                break;
            }
            int slot = emptySlots.remove(0);
            chestInventory.setItem(slot, item);
            itemsPlaced++;
            plugin.getPluginLogger().finest("   -> 在槽位 " + slot + " 放入物品: " + item.getType() + " x " + item.getAmount());
        }
        plugin.getPluginLogger().finer("  -> 成功将 " + itemsPlaced + " 堆物品放入箱子。");
    }

    private void removeControlLore(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasLore()) {
            List<String> originalLore = meta.getLore();
            if (originalLore != null) {
                List<String> cleanedLore = originalLore.stream()
                        .filter(line -> !PROBABILITY_PATTERN.matcher(line).find() && !QUANTITY_PATTERN.matcher(line).find())
                        .collect(Collectors.toList());
                meta.setLore(cleanedLore.isEmpty() ? null : cleanedLore);
                item.setItemMeta(meta);
                plugin.getPluginLogger().finest("    - 清理了物品 " + item.getType() + " 的控制 Lore。");
            }
        }
    }

    private boolean isInventoryEmpty(@Nullable Inventory inventory) {
        if (inventory == null) return true;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != Material.AIR) return false;
        }
        return true;
    }
}