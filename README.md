定制插件，老板已允许开源
## 此插件适合摸金类，RPG伺服器，允许配置单个GUI来调整全局战利品箱的掉落

## 命令
/tgui 打开全局掉落GUI
/tplace 放置一个战利品箱
/treload 重载插件
/tinfo 插件信息

## 配置文件说明

本插件使用以下外部文件来存储配置和数据。这些文件位于服务器的 `plugins/TarkovBooty/` 目录下。

### 1. `config.yml`

这是插件的主要配置文件，用于调整核心功能和消息文本。

*   **用途**:
    *   设置战利品箱的刷新间隔时间（秒）。
    *   配置日志输出级别。
    *   启用/禁用并配置刷新倒计时广播及其消息格式。
    *   启用/禁用并配置箱子开启延迟及其相关消息。
*   **示例**:
    ```yaml
    # config.yml Example

    # 日志级别 (SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST)
    log-level: INFO

    # 特殊战利品箱自动刷新的间隔时间 (单位: 秒)
    chest-refresh-interval: 300

    # 是否在刷新前进行全服倒计时广播
    broadcast-countdown: true
    # 倒计时广播的消息格式 ({seconds} 会被替换为剩余秒数)
    countdown-message: "&e[TarkovBooty] 战利品箱将在 &c{seconds} &e秒后刷新！"

    # 是否启用开启箱子时的延迟 (需要玩家按住右键一段时间)
    enable-opening-delay: true
    # 开启延迟的持续时间 (单位: 秒)
    opening-delay-seconds: 3
    # 开始尝试开启时的提示消息
    opening-start-message: "&e正在尝试开启战利品箱... 请保持不动！"
    # 因移动而取消开启时的提示消息
    opening-cancel-move-message: "&c移动取消了箱子开启！"
    # 当玩家已在尝试开启另一个箱子时的提示消息
    opening-already-opening-message: "&c你已经在尝试开启一个箱子了！"
    ```

### 2. `gui_data.yml`

此文件存储通过 `/tgui` 命令创建和编辑的战利品模板。模板中的物品列表直接存储 Bukkit ItemStack 的序列化数据。

*   **用途**:
    *   定义不同的战利品模板，每个模板包含一组可能刷出的物品。
    *   插件会从模板定义的物品列表中随机选择物品来填充特殊战利品箱（具体选择逻辑可能在代码中实现，例如随机抽取一个或多个）。
*   **注意**: 不建议手动编辑此文件，请尽量使用 `/tgui` 命令进行管理。物品的序列化数据（如 `v: 3953`）会因 Minecraft 和 Spigot 版本而变化。
*   **示例**: (基于你提供的结构)
    ```yaml
    # gui_data.yml Example

    templates:
        items: 
          '0':
            ==: org.bukkit.inventory.ItemStack
            v: 3953 
            type: STONE_AXE
          '1':
            ==: org.bukkit.inventory.ItemStack
            v: 3953
            type: WOODEN_AXE
          '2':
            ==: org.bukkit.inventory.ItemStack
            v: 3953
            type: NETHERITE_SWORD
          '3':
            ==: org.bukkit.inventory.ItemStack
            v: 3953
            type: DIAMOND_SWORD
          '4':
            ==: org.bukkit.inventory.ItemStack
            v: 3953
            type: GOLDEN_SWORD
          '5':
            ==: org.bukkit.inventory.ItemStack
            v: 3953
            type: IRON_SWORD
          '6':
            ==: org.bukkit.inventory.ItemStack
            v: 3953
            type: STONE_SWORD
          '7':
            ==: org.bukkit.inventory.ItemStack
            v: 3953
            type: WOODEN_SWORD
    ```

### 3. `chests.yml`

此文件记录了所有被 `/tplace` 命令标记为特殊战利品箱的位置及其关联的战利品模板。

*   **用途**:
    *   持久化存储特殊战利品箱的位置信息（世界、坐标）。
    *   记录每个箱子应该使用哪个 `gui_data.yml` 中定义的模板来刷新物品。
*   **注意**: 不建议手动编辑此文件，除非你需要精确删除某个不再需要的箱子记录。错误修改可能导致插件无法找到或刷新箱子。
*   **示例**:
    ```yaml
    # chests.yml Example

    chests:
      # 每个键是一个唯一标识符 (通常是 WorldName;X;Y;Z)
      'world;100;65;-50':
        template: "weapon_cache" # 关联的 gui_data.yml 中的模板名称
        location: # Spigot Location 的序列化表示
          ==: org.bukkit.Location
          world: "world" # 世界名称
          x: 100.5 # X 坐标 (通常是方块中心)
          y: 65.0  # Y 坐标
          z: -50.5 # Z 坐标 (通常是方块中心)
          pitch: 0.0
          yaw: 0.0
      'world_nether;-200;80;150':
        template: "another_template" # 假设存在另一个模板
        location:
          ==: org.bukkit.Location
          world: "world_nether"
          x: -200.5
          y: 80.0
          z: 150.5
          pitch: 0.0
          yaw: 0.0
      # ... 其他箱子
    ```
# 多多支持我的闲鱼店铺
【闲鱼】https://m.tb.cn/h.6nA3DRE?tk=7ACpVUFVHJa MF937 「这是我的闲鱼号，快来看看吧～」
点击链接直接打开

## 许可证

本项目采用 [知识共享署名-非商业性使用4.0 国际许可证]（https://creativecommons.org/licenses/by-nc/4.0/） 进行许可。
