package com.eogd.tarkovbooty;

import com.eogd.tarkovbooty.commands.TGuiCommand;
import com.eogd.tarkovbooty.commands.TInfoCommand;
import com.eogd.tarkovbooty.commands.TPlaceCommand;
import com.eogd.tarkovbooty.commands.TReloadCommand;
import com.eogd.tarkovbooty.listeners.BlockListener;
import com.eogd.tarkovbooty.listeners.ChestInteractListener;
import com.eogd.tarkovbooty.listeners.InventoryListener;
import com.eogd.tarkovbooty.managers.ChestManager;
import com.eogd.tarkovbooty.managers.ConfigManager;
import com.eogd.tarkovbooty.managers.DataManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class TarkovBooty extends JavaPlugin {

    private Logger pluginLogger;

    private ConfigManager configManager;
    private DataManager dataManager;
    private ChestManager chestManager;

    private InventoryListener inventoryListener;
    private BlockListener blockListener;
    private ChestInteractListener chestInteractListener;


    @Override
    public void onEnable() {
        pluginLogger = Logger.getLogger("TarkovBooty");
        pluginLogger.setParent(this.getLogger());
        pluginLogger.setLevel(Level.INFO);

        pluginLogger.info("正在初始化 TarkovBooty...");

        pluginLogger.fine("正在初始化 ConfigManager...");
        configManager = new ConfigManager(this);

        pluginLogger.fine("正在初始化 DataManager...");
        dataManager = new DataManager(this);
        dataManager.loadGuiData();
        dataManager.loadChestLocations();

        pluginLogger.fine("正在初始化 ChestManager...");
        chestManager = new ChestManager(this);
        chestManager.startRefreshTask();

        pluginLogger.fine("正在初始化监听器...");
        inventoryListener = new InventoryListener(this);
        blockListener = new BlockListener(this);
        chestInteractListener = new ChestInteractListener(this);

        pluginLogger.fine("正在注册监听器...");
        getServer().getPluginManager().registerEvents(inventoryListener, this);
        getServer().getPluginManager().registerEvents(blockListener, this);
        getServer().getPluginManager().registerEvents(chestInteractListener, this);

        pluginLogger.fine("正在注册命令...");
        registerCommands();

        pluginLogger.info("TarkovBooty v" + getDescription().getVersion() + " 已成功启用！");
    }

    @Override
    public void onDisable() {
        getLogger().info("正在禁用 TarkovBooty...");

        if (chestManager != null) {
            getLogger().info("正在停止 ChestManager 任务...");
            chestManager.stopRefreshTask();
        } else {
            getLogger().fine("ChestManager 为空，跳过任务停止。");
        }

        if (chestInteractListener != null) {
            getLogger().info("正在取消待处理的箱子开启任务...");
            chestInteractListener.cancelAllOpeningTasks();
        } else {
            getLogger().fine("ChestInteractListener 为空，跳过开启任务取消。");
        }

        if (dataManager != null) {
            getLogger().info("正在执行最终的箱子位置保存...");
            try {
                dataManager.saveChestLocations();
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "在禁用时保存数据出错:", e);
            }
        } else {
            getLogger().fine("DataManager 为空，跳过最终保存。");
        }

        getLogger().fine("正在清理资源...");
        configManager = null;
        dataManager = null;
        chestManager = null;
        inventoryListener = null;
        blockListener = null;
        chestInteractListener = null;
        pluginLogger = null;

        getLogger().info("TarkovBooty 已禁用。");
    }

    private void registerCommands() {
        PluginCommand tguiCmd = getCommand("tgui");
        PluginCommand tplaceCmd = getCommand("tplace");
        PluginCommand treloadCmd = getCommand("treload");
        PluginCommand tinfoCmd = getCommand("tinfo");

        Logger logger = getPluginLogger();

        if (tguiCmd != null) {
            tguiCmd.setExecutor(new TGuiCommand(this));
            logger.fine("已为 /tgui 注册执行器");
        } else {
            logger.severe("注册命令 /tgui 失败 - 请检查 plugin.yml！");
        }

        if (tplaceCmd != null) {
            tplaceCmd.setExecutor(new TPlaceCommand(this));
            logger.fine("已为 /tplace 注册执行器");
        } else {
            logger.severe("注册命令 /tplace 失败 - 请检查 plugin.yml！");
        }

        if (treloadCmd != null) {
            treloadCmd.setExecutor(new TReloadCommand(this));
            logger.fine("已为 /treload 注册执行器");
        } else {
            logger.severe("注册命令 /treload 失败 - 请检查 plugin.yml！");
        }

        if (tinfoCmd != null) {
            tinfoCmd.setExecutor(new TInfoCommand(this));
            logger.fine("已为 /tinfo 注册执行器");
        } else {
            logger.severe("注册命令 /tinfo 失败 - 请检查 plugin.yml！");
        }
    }

    public ConfigManager getConfigManager() { return configManager; }
    public DataManager getDataManager() { return dataManager; }
    public ChestManager getChestManager() { return chestManager; }
    public Logger getPluginLogger() {
        if (pluginLogger == null) {
            Logger defaultLogger = this.getLogger();
            defaultLogger.warning("尝试访问 pluginLogger 时其为空！将返回默认 logger。");
            return defaultLogger;
        }
        return pluginLogger;
    }
    // public ChestInteractListener getChestInteractListener() { return chestInteractListener; }
}