# PostBits

一个模块化的 Minecraft Spigot 插件，集成了多个小功能。

## 特性

- **模块化设计**: 所有功能均可独立开关
- **默认关闭**: 所有模块默认关闭，需要手动启用
- **全 i18n 支持**: 支持多语言（中文/英文）
- **调试信息**: 调试信息使用英文硬编码
- **更新检查**: 自动检查 GitHub 上的最新版本
- **重载功能**: 支持热重载配置
- **插件集成**: 支持 CraftEngine、ItemsAdder、Oraxen 等自定义方块插件

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

## 模块列表

- **Chair（椅子）** - 可坐在楼梯、台阶或自定义方块上
- **InvEdit（背包编辑）** - 编辑其他玩家的背包
- **Elevator（电梯）** - 使用特定方块快速上下传送
- **Utility（实用命令）** - heal/fix/hat/speed/fly/vanish 等实用命令
- **PortableTools（随身工具）** - 便携工作台、砂轮、附魔台等
- **KeyBind（按键绑定）** - 自定义按键触发的动作（需要 ProtocolLib）
- **Biome（生物群系）** - 进入特定生物群系时触发动作（支持 1.13-1.21+ 原版生物群系）

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

## 克隆

```
git clone --recursive https://github.com/postyizhan/PostBits.git
```
