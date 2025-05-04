package com.eogd.tarkovbooty.managers;

import com.eogd.tarkovbooty.TarkovBooty;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.logging.Level;

public class ConfigManager {

    private final TarkovBooty plugin;
    private FileConfiguration config;

    private Level logLevel;
    private int chestRefreshInterval;
    private boolean broadcastCountdown;
    private String countdownMessage;
    private boolean enableOpeningDelay;
    private int openingDelaySeconds;
    private String openingStartMessage;
    private String openingCancelMoveMessage;
    private String openingAlreadyOpeningMessage;


    public ConfigManager(TarkovBooty plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        String levelName = config.getString("log-level", "INFO").toUpperCase();
        try {
            logLevel = Level.parse(levelName);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("无效的日志级别 '" + levelName + "'，使用 INFO。");
            logLevel = Level.INFO;
        }
        plugin.getPluginLogger().setLevel(logLevel);
        plugin.getLogger().info("日志级别: " + logLevel.getName());

        chestRefreshInterval = config.getInt("chest-refresh-interval", 300);
        plugin.getPluginLogger().config("箱子刷新间隔: " + chestRefreshInterval + " 秒");

        broadcastCountdown = config.getBoolean("broadcast-countdown", true);
        plugin.getPluginLogger().config("广播倒计时: " + broadcastCountdown);
        countdownMessage = config.getString("countdown-message", "&e[TarkovBooty] 战利品箱将在 &c{seconds} &e秒后刷新！");
        plugin.getPluginLogger().config("倒计时消息: " + countdownMessage);

        enableOpeningDelay = config.getBoolean("enable-opening-delay", true);
        plugin.getPluginLogger().config("开启延迟启用: " + enableOpeningDelay);

        openingDelaySeconds = config.getInt("opening-delay-seconds", 3);
        plugin.getPluginLogger().config("开启延迟时间: " + openingDelaySeconds + " 秒");

        openingStartMessage = config.getString("opening-start-message", "&e正在尝试开启战利品箱... 请保持不动！");
        plugin.getPluginLogger().config("开启开始消息: " + openingStartMessage);

        openingCancelMoveMessage = config.getString("opening-cancel-move-message", "&c移动取消了箱子开启！");
        plugin.getPluginLogger().config("开启取消(移动)消息: " + openingCancelMoveMessage);

        openingAlreadyOpeningMessage = config.getString("opening-already-opening-message", "&c你已经在尝试开启一个箱子了！");
        plugin.getPluginLogger().config("已在开启消息: " + openingAlreadyOpeningMessage);


        if (plugin.getChestManager() != null) {
            plugin.getPluginLogger().info("检测到配置重载，重启箱子刷新任务...");
            plugin.getChestManager().startRefreshTask();
        } else {
            plugin.getPluginLogger().fine("ChestManager 未初始化，跳过任务重启。");
        }
    }

    public void reloadConfig() {
        loadConfig();
    }

    public Level getLogLevel() { return logLevel; }
    public int getChestRefreshInterval() { return chestRefreshInterval; }
    public boolean isBroadcastCountdownEnabled() { return broadcastCountdown; }
    public String getCountdownMessageFormat() { return countdownMessage; }
    public boolean isOpeningDelayEnabled() { return enableOpeningDelay; }
    public int getOpeningDelaySeconds() { return openingDelaySeconds; }
    public String getOpeningStartMessage() { return openingStartMessage; }
    public String getOpeningCancelMoveMessage() { return openingCancelMoveMessage; }
    public String getOpeningAlreadyOpeningMessage() { return openingAlreadyOpeningMessage; }
}