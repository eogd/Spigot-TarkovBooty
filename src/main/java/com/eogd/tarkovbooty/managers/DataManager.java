package com.eogd.tarkovbooty.managers;

import com.eogd.tarkovbooty.TarkovBooty;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class DataManager {

    private final TarkovBooty plugin;
    private File guiDataFile;
    private FileConfiguration guiDataConfig;
    private File chestsFile;
    private FileConfiguration chestsConfig;
    private Inventory guiTemplateInventory;
    private final Set<Location> specialChestLocations = new HashSet<>();
    private static final String GUI_TITLE = "战利品模板设置";
    private static final int GUI_SIZE = 54;


    public DataManager(TarkovBooty plugin) {
        this.plugin = plugin;

        setupFiles();
        this.guiTemplateInventory = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);
    }

    private void setupFiles() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        guiDataFile = new File(plugin.getDataFolder(), "gui_data.yml");
        if (!guiDataFile.exists()) {
            try {
                guiDataFile.createNewFile();
                plugin.getPluginLogger().info("创建了新的数据文件: gui_data.yml");
            } catch (IOException e) {
                plugin.getPluginLogger().log(Level.SEVERE, "无法创建 gui_data.yml 文件!", e);
            }
        }
        guiDataConfig = YamlConfiguration.loadConfiguration(guiDataFile);

        chestsFile = new File(plugin.getDataFolder(), "chests.yml");
        if (!chestsFile.exists()) {
            try {
                chestsFile.createNewFile();
                plugin.getPluginLogger().info("创建了新的数据文件: chests.yml");
            } catch (IOException e) {
                plugin.getPluginLogger().log(Level.SEVERE, "无法创建 chests.yml 文件!", e);
            }
        }
        chestsConfig = YamlConfiguration.loadConfiguration(chestsFile);
    }

    public void loadGuiData() {
        plugin.getPluginLogger().fine("正在从 gui_data.yml 加载 GUI 模板...");
        try {
            guiDataConfig.load(guiDataFile);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getPluginLogger().log(Level.SEVERE, "无法加载 gui_data.yml!", e);
            return;
        }

        guiTemplateInventory = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);
        ConfigurationSection itemsSection = guiDataConfig.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    if (slot >= 0 && slot < GUI_SIZE) {
                        ItemStack item = itemsSection.getItemStack(key);
                        if (item != null) {
                            guiTemplateInventory.setItem(slot, item);
                            plugin.getPluginLogger().finest(" - 在槽位 " + slot + " 加载物品: " + item.getType());
                        } else {
                            plugin.getPluginLogger().warning("在 gui_data.yml 的槽位 " + slot + " 发现 null 物品数据，已跳过。");
                        }
                    } else {
                        plugin.getPluginLogger().warning("在 gui_data.yml 发现无效的槽位索引 '" + key + "'，已跳过。");
                    }
                } catch (NumberFormatException e) {
                    plugin.getPluginLogger().warning("在 gui_data.yml 发现非数字的槽位索引 '" + key + "'，已跳过。");
                } catch (Exception e) {
                    plugin.getPluginLogger().log(Level.SEVERE, "加载槽位 " + key + " 的物品时出错: " + e.getMessage());
                }
            }
        } else {
            plugin.getPluginLogger().info("gui_data.yml 中没有找到 'items' 配置节，模板为空。");
        }
        plugin.getPluginLogger().fine("GUI 模板加载完成。");
    }

    public void saveGuiData(@NotNull Inventory inventoryToSave) {
        plugin.getPluginLogger().fine("正在将 GUI 模板保存到 gui_data.yml...");
        guiDataConfig.set("items", null);
        ConfigurationSection itemsSection = guiDataConfig.createSection("items");
        for (int i = 0; i < inventoryToSave.getSize(); i++) {
            ItemStack item = inventoryToSave.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                itemsSection.set(String.valueOf(i), item);
                plugin.getPluginLogger().finest(" - 在槽位 " + i + " 保存物品: " + item.getType());
            }
        }
        try {
            guiDataConfig.save(guiDataFile);
            plugin.getPluginLogger().fine("GUI 模板成功保存到 gui_data.yml。");
        } catch (IOException e) {
            plugin.getPluginLogger().log(Level.SEVERE, "无法保存 GUI 模板到 gui_data.yml!", e);
        }
    }

    @NotNull
    public Inventory getGuiTemplateInventory() {
        if (guiTemplateInventory == null) {
            plugin.getPluginLogger().warning("getGuiTemplateInventory() 被调用时 guiTemplateInventory 为 null，返回一个新的空 Inventory。");
            guiTemplateInventory = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);
        }
        return guiTemplateInventory;
    }

    @NotNull
    public static String getGuiTitle() {
        return GUI_TITLE;
    }

    public void loadChestLocations() {
        plugin.getPluginLogger().fine("正在从 chests.yml 加载特殊箱子位置...");
        try {
            chestsConfig.load(chestsFile);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getPluginLogger().log(Level.SEVERE, "无法加载 chests.yml!", e);
            return;
        }

        specialChestLocations.clear();
        List<String> locationStrings = chestsConfig.getStringList("locations");

        if (locationStrings.isEmpty() && chestsConfig.contains("locations")) {
            plugin.getPluginLogger().info("chests.yml 中的 'locations' 列表为空。");
        } else if (!chestsConfig.contains("locations")) {
            plugin.getPluginLogger().info("chests.yml 中没有找到 'locations' 配置节，没有箱子位置被加载。");
        }


        for (String locString : locationStrings) {
            Location loc = deserializeLocation(locString);
            if (loc != null) {
                specialChestLocations.add(loc);
                plugin.getPluginLogger().finest(" - 加载箱子位置: " + locString);
            } else {
                plugin.getPluginLogger().warning("无法解析 chests.yml 中的位置字符串: '" + locString + "'，已跳过。");
            }
        }
        plugin.getPluginLogger().fine("特殊箱子位置加载完成，共 " + specialChestLocations.size() + " 个。");
    }

    public void saveChestLocations() {
        plugin.getPluginLogger().fine("正在将特殊箱子位置保存到 chests.yml...");
        List<String> locationStrings = specialChestLocations.stream()
                .map(this::serializeLocation)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        chestsConfig.set("locations", locationStrings);

        try {
            chestsConfig.save(chestsFile);
            plugin.getPluginLogger().fine("特殊箱子位置成功保存到 chests.yml，共 " + locationStrings.size() + " 个。");
        } catch (IOException e) {
            plugin.getPluginLogger().log(Level.SEVERE, "无法保存特殊箱子位置到 chests.yml!", e);
        }
    }

    public boolean addChestLocation(@NotNull Location location) {
        Location blockLocation = location.getBlock().getLocation();
        boolean added = specialChestLocations.add(blockLocation);
        if (added) {
            plugin.getPluginLogger().fine("添加新的特殊箱子位置: " + serializeLocation(blockLocation));
        }
        return added;
    }

    public boolean removeChestLocation(@NotNull Location location) {
        Location blockLocation = location.getBlock().getLocation();
        boolean removed = specialChestLocations.remove(blockLocation);
        if (removed) {
            plugin.getPluginLogger().fine("移除特殊箱子位置: " + serializeLocation(blockLocation));
        }
        return removed;
    }


    @NotNull
    public Set<Location> getSpecialChestLocations() {
        return new HashSet<>(specialChestLocations);
    }

    public boolean isSpecialChest(@NotNull Location location) {
        return specialChestLocations.contains(location.getBlock().getLocation());
    }

    @Nullable
    public String serializeLocation(@Nullable Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        return location.getWorld().getName() + "," +
                location.getBlockX() + "," +
                location.getBlockY() + "," +
                location.getBlockZ();
    }

    @Nullable
    private Location deserializeLocation(@Nullable String locString) {
        if (locString == null || locString.isEmpty()) {
            return null;
        }
        String[] parts = locString.split(",");
        if (parts.length == 4) {
            try {
                String worldName = parts[0];
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                int z = Integer.parseInt(parts[3]);

                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    return new Location(world, x, y, z);
                } else {
                    plugin.getPluginLogger().warning("无法找到名为 '" + worldName + "' 的世界，位置 '" + locString + "' 加载失败。");
                    return null;
                }
            } catch (NumberFormatException e) {
                plugin.getPluginLogger().warning("解析位置坐标时出错: '" + locString + "' - " + e.getMessage());
                return null;
            }
        }
        return null;
    }
}