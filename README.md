# PostBits

一个模块化的 Minecraft Spigot 插件，集成了多个小功能。

## 特性

- **模块化设计**: 所有功能均可独立开关
- **默认关闭**: 所有模块默认关闭，需要手动启用
- **全 i18n 支持**: 支持多语言（中文/英文）
- **调试信息**: 调试信息使用英文硬编码
- **更新检查**: 自动检查 GitHub 上的最新版本
- **重载功能**: 支持热重载配置

## 当前模块

### 更新检查模块
- 自动检查 GitHub 上的最新版本
- 可配置检查频率
- 支持手动检查更新

### 椅子模块
- 右键方块坐下功能
- 可配置可坐方块类型
- 支持楼梯、台阶等多种方块
- 潜行起身、受伤自动起身
- 安全检查和距离限制
- 自动调整坐下高度，避免浮空

### 背包编辑模块
- 管理员编辑玩家背包功能
- 图形化界面编辑背包、装备、副手
- 权限控制和免疫系统
- 实时保存和取消功能
- 安全检查和通知系统

### 电梯模块
- 站在电梯方块上跳跃或蹲下进行楼层传送
- 支持多种电梯方块类型配置
- 自动寻找上下楼层，智能空间检测
- 音效、粒子效果和消息提示
- 冷却时间防止频繁使用
- **纯事件驱动，无需任何命令**

### 头部装备模块
- 将手上的物品戴在头上作为装饰
- 自动交换手上和头上的物品
- 简单易用，一个命令搞定

### 随身工具模块
- 便携工具合集，随时随地使用各种工作台
- 支持工作台、砂轮、制图机、附魔台、锻造台、末影箱
- 无需放置方块，直接通过命令打开界面
- 完整的原版功能支持和权限控制

## 安装

1. 下载最新的 JAR 文件
2. 将文件放入服务器的 `plugins` 文件夹
3. 重启服务器
4. 编辑 `plugins/PostBits/config.yml` 启用需要的模块

## 配置

### 主配置文件 (config.yml)

```yaml
# 模块开关
modules:
  update-checker:
    enabled: false  # 设置为 true 启用更新检查
    check-interval-days: 1  # 检查频率（天）

  chair:
    enabled: false  # 设置为 true 启用椅子功能
    sittable-blocks:  # 可坐方块类型
      - "oak_stairs"
      - "oak_slab"
      # ... 更多方块类型
    empty-hand-only: true  # 是否需要空手
    max-distance: 3.0  # 最大坐下距离
    allow-unsafe: false  # 是否允许不安全位置
    sneak-to-stand: true  # 潜行起身
    stand-on-damage: true  # 受伤起身

  invedit:
    enabled: false  # 设置为 true 启用背包编辑功能

  elevator:
    enabled: false  # 设置为 true 启用电梯功能
    max-search-height: 50  # 向上搜索的最大高度
    max-search-depth: 50   # 向下搜索的最大深度
    min-floor-distance: 2  # 楼层之间的最小距离
    cooldown-time: 500     # 冷却时间（毫秒）
    elevator-blocks:       # 电梯方块类型
      - "IRON_BLOCK"
      - "GOLD_BLOCK"
      - "DIAMOND_BLOCK"
      # ... 更多方块类型
    sound-enabled: true    # 音效开关
    particle-enabled: true # 粒子效果开关

  head:
    enabled: false  # 设置为 true 启用头部装备功能

  portabletools:
    enabled: false  # 设置为 true 启用随身工具功能

# 语言设置
language: zh_CN  # 支持 zh_CN, en_US

# 调试模式
debug: false
```

## 命令

- `/postbits` 或 `/pb` - 显示帮助信息
- `/postbits reload` - 重载插件配置
- `/postbits update` - 检查插件更新（需要启用更新检查模块）
- `/postbits chair [sit|stand|info]` - 椅子功能（需要启用椅子模块）
- `/postbits invedit <玩家名>` - 背包编辑功能（需要启用背包编辑模块）
- `/postbits head [remove]` - 头部装备功能（需要启用头部装备模块）
- `/postbits craft` - 打开便携工作台（需要启用随身工具模块）
- `/postbits grindstone` - 打开便携砂轮（需要启用随身工具模块）
- `/postbits cartography` - 打开便携制图机（需要启用随身工具模块）
- `/postbits enchanting` - 打开便携附魔台（需要启用随身工具模块）
- `/postbits smithing` - 打开便携锻造台（需要启用随身工具模块）
- `/postbits enderchest` - 打开便携末影箱（需要启用随身工具模块）

## 权限

- `postbits.admin` - 所有管理权限
- `postbits.admin.reload` - 重载权限
- `postbits.admin.update` - 更新检查权限
- `postbits.chair.sit` - 椅子坐下权限
- `postbits.chair.info` - 椅子信息查看权限
- `postbits.invedit.use` - 背包编辑使用权限
- `postbits.elevator.use` - 电梯使用权限
- `postbits.head.use` - 头部装备使用权限
- `postbits.portabletools.craft` - 便携工作台使用权限
- `postbits.portabletools.grindstone` - 便携砂轮使用权限
- `postbits.portabletools.cartography` - 便携制图机使用权限
- `postbits.portabletools.enchanting` - 便携附魔台使用权限
- `postbits.portabletools.smithing` - 便携锻造台使用权限
- `postbits.portabletools.enderchest` - 便携末影箱使用权限

## 椅子功能使用方法

### 基本使用
1. 启用椅子模块：在配置文件中设置 `modules.chair.enabled: true`
2. 重载插件：`/postbits reload`
3. 右键点击楼梯或台阶等可坐方块即可坐下
4. 潜行或使用命令 `/postbits chair stand` 起身

### 配置可坐方块
在配置文件的 `modules.chair.sittable-blocks` 中添加方块类型：
```yaml
sittable-blocks:
  - "oak_stairs"
  - "stone_stairs"
  - "oak_slab"
  - "stone_slab"
  # 添加更多方块类型...
```

### 安全设置
- `allow-unsafe: false` - 禁止在不安全位置（上方有方块）坐下
- `max-distance: 3.0` - 限制坐下的最大距离
- `empty-hand-only: true` - 要求空手才能坐下

## 背包编辑功能使用方法

### 基本使用
1. 启用背包编辑模块：在配置文件中设置 `modules.invedit.enabled: true`
2. 重载插件：`/postbits reload`
3. 使用命令 `/postbits invedit <玩家名>` 打开玩家背包编辑界面
4. 在GUI界面中直接编辑物品，点击绿色方块保存更改

### 可用命令
- `/postbits invedit <玩家名>` - 编辑指定玩家的背包

### GUI界面说明
- 槽位 0-35：背包物品
- 槽位 36-39：盔甲（靴子/护腿/胸甲/头盔）
- 槽位 40：副手物品
- 绿色方块：保存更改
- 红色方块：取消编辑

### 权限设置
- `postbits.invedit.use` - 允许编辑玩家背包

### 功能特点
- 不能编辑自己的背包（安全限制）
- 不会通知目标玩家被编辑（静默操作）
- 任何在线玩家的背包都可以被编辑（无免疫机制）

## 电梯功能使用方法

### 基本使用
1. 启用电梯模块：在配置文件中设置 `modules.elevator.enabled: true`
2. 重载插件：`/postbits reload`
3. 在同一水平位置的不同高度放置电梯方块
4. 站在电梯方块上**跳跃** → 传送到上方楼层
5. 站在电梯方块上**蹲下** → 传送到下方楼层

### 电梯方块配置
默认支持的电梯方块类型：
- 铁块 (IRON_BLOCK)
- 金块 (GOLD_BLOCK)  
- 钻石块 (DIAMOND_BLOCK)
- 绿宝石块 (EMERALD_BLOCK)
- 石英块 (QUARTZ_BLOCK)
- 青金石块 (LAPIS_BLOCK)
- 红石块 (REDSTONE_BLOCK)
- 煤炭块 (COAL_BLOCK)
- 铜块 (COPPER_BLOCK)
- 下界合金块 (NETHERITE_BLOCK)

### 配置选项说明
- `max-search-height: 50` - 向上搜索电梯的最大高度
- `max-search-depth: 50` - 向下搜索电梯的最大深度
- `min-floor-distance: 2` - 楼层之间的最小距离
- `cooldown-time: 500` - 使用冷却时间（毫秒）
- `sound-enabled: true` - 是否播放传送音效
- `particle-enabled: true` - 是否显示粒子效果
- `message-enabled: true` - 是否显示传送消息

### 权限设置
- `postbits.elevator.use` - 允许使用电梯功能

### 使用注意事项
- 电梯方块必须在同一水平位置（相同的X、Z坐标）
- 楼层之间需要保持最小距离（默认2格）
- 目标位置必须有足够的空间容纳玩家（2格高度）
- 存在冷却时间防止频繁使用
- **完全通过跳跃和蹲下操作，无需输入任何命令**

## 头部装备功能使用方法

### 基本使用
1. 启用头部装备模块：在配置文件中设置 `modules.head.enabled: true`
2. 重载插件：`/postbits reload`
3. 手持物品，输入 `/postbits head` 戴在头上
4. 输入 `/postbits head remove` 取下

### 权限设置
- `postbits.head.use` - 允许使用头部装备功能

## 随身工具功能使用方法

### 基本使用
1. 启用随身工具模块：在配置文件中设置 `modules.portabletools.enabled: true`
2. 重载插件：`/postbits reload`
3. 使用对应命令打开各种便携工具界面

### 支持的工具
- **便携工作台** - `/postbits craft` - 随时随地合成物品 ✅
- **便携附魔台** - `/postbits enchanting` - 附魔装备和工具 ✅  
- **便携末影箱** - `/postbits enderchest` - 访问个人存储 ✅
- **便携砂轮** - `/postbits grindstone` - 修复和分解物品 🔄（需要1.14+）
- **便携制图机** - `/postbits cartography` - 制作和复制地图 🔄（需要1.14+）
- **便携锻造台** - `/postbits smithing` - 升级钻石装备 🔄（需要1.16+）

### 特色功能
- **无需放置方块** - 直接通过命令打开界面
- **智能版本兼容** - 自动检测服务器版本，支持的工具才启用
- **完整功能支持** - 所有支持的工具都具备完整的原版功能
- **权限控制** - 每个工具都有独立的权限设置
- **即时访问** - 无冷却时间，随时可用
- **优雅降级** - 不支持的功能会友好提示版本要求

### 权限设置
- `postbits.portabletools.craft` - 允许使用便携工作台
- `postbits.portabletools.grindstone` - 允许使用便携砂轮
- `postbits.portabletools.cartography` - 允许使用便携制图机
- `postbits.portabletools.enchanting` - 允许使用便携附魔台
- `postbits.portabletools.smithing` - 允许使用便携锻造台
- `postbits.portabletools.enderchest` - 允许使用便携末影箱

### 版本兼容性
- **Spigot 1.13+** - 支持工作台、附魔台、末影箱
- **Spigot 1.14+** - 额外支持砂轮、制图机
- **Spigot 1.16+** - 额外支持锻造台
- **智能检测** - 插件启动时自动检测并记录可用功能

### 使用注意事项
- 附魔台需要消耗经验值和青金石，与原版相同
- 锻造台需要相应的锻造模板和材料（仅1.16+）
- 末影箱访问的是玩家个人的末影箱存储空间
- 不支持的工具会显示版本要求提示
- 所有操作都会正常触发相关的服务器事件

## 开发

### 构建

```bash
./gradlew build
```

### 项目结构

```
src/main/
├── kotlin/com/github/postyizhan/
│   ├── PostBits.kt              # 主类
│   ├── chair/                   # 椅子模块
│   │   ├── ChairService.kt      # 椅子服务
│   │   ├── ChairEventHandler.kt # 事件处理
│   │   ├── ChairCommand.kt      # 命令处理
│   │   └── ChairSeat.kt         # 座位数据
│   ├── invedit/                 # 背包编辑模块
│   │   ├── InvEditService.kt    # 背包编辑服务
│   │   ├── InvEditEventHandler.kt # 事件处理
│   │   └── InvEditCommand.kt    # 命令处理
│   ├── elevator/                # 电梯模块
│   │   ├── ElevatorService.kt   # 电梯服务
│   │   └── ElevatorEventHandler.kt # 事件处理
│   ├── head/                    # 头部装备模块
│   │   ├── HeadService.kt       # 头部装备服务
│   │   └── HeadCommand.kt       # 命令处理
│   ├── portabletools/           # 随身工具模块
│   │   ├── PortableToolsService.kt # 随身工具服务
│   │   └── PortableToolsCommand.kt # 命令处理
│   ├── command/
│   │   └── CommandManager.kt    # 命令管理器
│   ├── config/
│   │   └── ConfigManager.kt     # 配置管理器
│   └── util/
│       ├── MessageUtil.kt       # 消息工具类
│       └── UpdateChecker.kt     # 更新检查器
└── resources/
    ├── config.yml               # 默认配置
    ├── plugin.yml               # 插件描述
    └── lang/                    # 语言文件
        ├── zh_CN.yml
        └── en_US.yml
```

## 许可证

MIT License

## 作者

postyizhan