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

## 权限

- `postbits.admin` - 所有管理权限
- `postbits.admin.reload` - 重载权限
- `postbits.admin.update` - 更新检查权限
- `postbits.chair.sit` - 椅子坐下权限
- `postbits.chair.info` - 椅子信息查看权限

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
