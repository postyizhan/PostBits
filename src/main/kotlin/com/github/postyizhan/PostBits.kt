package com.github.postyizhan

import com.github.postyizhan.chair.ChairEventHandler
import com.github.postyizhan.chair.ChairService
import com.github.postyizhan.command.CommandManager
import com.github.postyizhan.config.ConfigManager
import com.github.postyizhan.elevator.ElevatorEventHandler
import com.github.postyizhan.elevator.ElevatorService
import com.github.postyizhan.head.HeadService
import com.github.postyizhan.invedit.InvEditEventHandler
import com.github.postyizhan.invedit.InvEditService
import com.github.postyizhan.util.MessageUtil
import com.github.postyizhan.util.UpdateChecker
import org.bukkit.command.CommandSender

import org.bukkit.plugin.java.JavaPlugin

/**
 * PostBits 主类 - 模块化小功能集成插件
 *
 * @author postyizhan
 */
class PostBits : JavaPlugin() {

    private lateinit var configManager: ConfigManager
    private lateinit var commandManager: CommandManager
    private lateinit var updateChecker: UpdateChecker
    private lateinit var chairService: ChairService
    private lateinit var chairEventHandler: ChairEventHandler
    private lateinit var invEditService: InvEditService
    private lateinit var invEditEventHandler: InvEditEventHandler
    private lateinit var elevatorService: ElevatorService
    private lateinit var elevatorEventHandler: ElevatorEventHandler
    private lateinit var headService: HeadService
    private var debugEnabled: Boolean = false

    companion object {
        private lateinit var instance: PostBits

        /**
         * 获取插件实例
         */
        fun getInstance(): PostBits {
            return instance
        }
    }

    /**
     * 插件启用时触发
     */
    override fun onEnable() {
        instance = this

        // 初始化配置管理器
        configManager = ConfigManager(this)
        configManager.loadAll()

        // 初始化消息工具
        MessageUtil.init(this)

        // 获取调试模式设置
        debugEnabled = configManager.getConfig().getBoolean("debug", false)

        // 初始化命令管理器
        commandManager = CommandManager(this)
        getCommand("postbits")?.setExecutor(commandManager)
        getCommand("postbits")?.tabCompleter = commandManager



        // 初始化椅子服务
        if (configManager.getConfig().getBoolean("modules.chair.enabled", false)) {
            chairService = ChairService(this)
            chairEventHandler = ChairEventHandler(this, chairService)
            server.pluginManager.registerEvents(chairEventHandler, this)
            if (debugEnabled) {
                logger.info("Debug: Chair module enabled")
            }
        }

        // 初始化背包编辑服务
        if (configManager.getConfig().getBoolean("modules.invedit.enabled", false)) {
            invEditService = InvEditService(this)
            invEditEventHandler = InvEditEventHandler(this, invEditService)
            server.pluginManager.registerEvents(invEditEventHandler, this)
            if (debugEnabled) {
                logger.info("Debug: InvEdit module enabled")
            }
        }

        // 初始化电梯服务
        if (configManager.getConfig().getBoolean("modules.elevator.enabled", false)) {
            elevatorService = ElevatorService(this)
            elevatorEventHandler = ElevatorEventHandler(this, elevatorService)
            server.pluginManager.registerEvents(elevatorEventHandler, this)
            if (debugEnabled) {
                logger.info("Debug: Elevator module enabled")
            }
        }

        // 初始化头部装备服务
        if (configManager.getConfig().getBoolean("modules.head.enabled", false)) {
            headService = HeadService(this)
            headService.initialize()
            if (debugEnabled) {
                logger.info("Debug: Head module enabled")
            }
        }

        // 初始化更新检查器
        if (configManager.getConfig().getBoolean("modules.update-checker.enabled", false)) {
            updateChecker = UpdateChecker(this, "postyizhan/PostBits")
            updateChecker.checkForUpdates { isUpdateAvailable, newVersion ->
                if (isUpdateAvailable) {
                    server.consoleSender.sendMessage(MessageUtil.color(
                        MessageUtil.getMessage("system.updater.update_available")
                            .replace("{current_version}", description.version)
                            .replace("{latest_version}", newVersion)
                    ))
                    server.consoleSender.sendMessage(MessageUtil.color(
                        MessageUtil.getMessage("system.updater.update_url")
                    ))
                } else {
                    if (debugEnabled) {
                        logger.info("Debug: Plugin is up to date (${description.version})")
                    }
                    server.consoleSender.sendMessage(MessageUtil.color(
                        MessageUtil.getMessage("system.updater.up_to_date")
                    ))
                }
            }
        }

        // 输出启用消息
        server.consoleSender.sendMessage(MessageUtil.color(MessageUtil.getMessage("messages.enabled")))
        if (debugEnabled) {
            logger.info("Debug: PostBits plugin enabled successfully")
        }
    }

    /**
     * 插件禁用时触发
     */
    override fun onDisable() {
        // 清理椅子服务
        if (this::chairService.isInitialized) {
            chairService.cleanup()
        }

        // 清理背包编辑服务
        if (this::invEditService.isInitialized) {
            invEditService.cleanup()
        }

        // 清理电梯服务
        if (this::elevatorService.isInitialized) {
            elevatorService.cleanup()
        }

        // 清理头部装备服务
        if (this::headService.isInitialized) {
            headService.cleanup()
        }

        // 输出禁用消息
        server.consoleSender.sendMessage(MessageUtil.color(MessageUtil.getMessage("messages.disabled")))
        if (debugEnabled) {
            logger.info("Debug: PostBits plugin disabled")
        }
    }



    /**
     * 重新加载插件配置
     */
    fun reload() {
        configManager.loadAll()
        MessageUtil.init(this)
        debugEnabled = configManager.getConfig().getBoolean("debug", false)

        // 重新初始化更新检查器
        if (configManager.getConfig().getBoolean("modules.update-checker.enabled", false)) {
            if (!this::updateChecker.isInitialized) {
                updateChecker = UpdateChecker(this, "postyizhan/PostBits")
            }
        }

        // 重新初始化椅子服务
        if (configManager.getConfig().getBoolean("modules.chair.enabled", false)) {
            if (!this::chairService.isInitialized) {
                chairService = ChairService(this)
                chairEventHandler = ChairEventHandler(this, chairService)
                server.pluginManager.registerEvents(chairEventHandler, this)
            }
        } else if (this::chairService.isInitialized) {
            // 如果椅子模块被禁用，清理现有座位
            chairService.cleanup()
        }

        // 重新初始化背包编辑服务
        if (configManager.getConfig().getBoolean("modules.invedit.enabled", false)) {
            if (!this::invEditService.isInitialized) {
                invEditService = InvEditService(this)
                invEditEventHandler = InvEditEventHandler(this, invEditService)
                server.pluginManager.registerEvents(invEditEventHandler, this)
            }
        } else if (this::invEditService.isInitialized) {
            // 如果背包编辑模块被禁用，清理现有会话
            invEditService.cleanup()
        }

        // 重新初始化电梯服务
        if (configManager.getConfig().getBoolean("modules.elevator.enabled", false)) {
            if (!this::elevatorService.isInitialized) {
                elevatorService = ElevatorService(this)
                elevatorEventHandler = ElevatorEventHandler(this, elevatorService)
                server.pluginManager.registerEvents(elevatorEventHandler, this)
            }
        } else if (this::elevatorService.isInitialized) {
            // 如果电梯模块被禁用，清理现有数据
            elevatorService.cleanup()
        }

        // 重新初始化头部装备服务
        if (configManager.getConfig().getBoolean("modules.head.enabled", false)) {
            if (!this::headService.isInitialized) {
                headService = HeadService(this)
                headService.initialize()
            }
        } else if (this::headService.isInitialized) {
            // 如果头部装备模块被禁用，清理现有数据
            headService.cleanup()
        }

        if (debugEnabled) {
            logger.info("Debug: Plugin configuration reloaded")
        }
    }

    /**
     * 向命令发送者发送更新检查信息
     */
    fun sendUpdateInfo(sender: CommandSender) {
        if (!this::updateChecker.isInitialized) {
            MessageUtil.sendMessage(sender, "messages.module_disabled")
            return
        }

        // 显示检查中的消息
        MessageUtil.sendMessage(sender, "system.updater.update_checking")

        updateChecker.checkForUpdates { isUpdateAvailable, newVersion ->
            if (isUpdateAvailable) {
                val updateAvailableMsg = MessageUtil.getMessage("system.updater.update_available")
                    .replace("{current_version}", description.version)
                    .replace("{latest_version}", newVersion)

                val updateUrlMsg = MessageUtil.getMessage("system.updater.update_url")

                sender.sendMessage(MessageUtil.color(updateAvailableMsg))
                sender.sendMessage(MessageUtil.color(updateUrlMsg))
            } else {
                val upToDateMsg = MessageUtil.getMessage("system.updater.up_to_date")
                sender.sendMessage(MessageUtil.color(upToDateMsg))
            }
        }
    }

    /**
     * 获取配置管理器
     */
    fun getConfigManager(): ConfigManager {
        return configManager
    }

    /**
     * 是否启用调试模式
     */
    fun isDebugEnabled(): Boolean {
        return debugEnabled
    }

    /**
     * 获取椅子服务
     */
    fun getChairService(): ChairService? {
        return if (this::chairService.isInitialized) chairService else null
    }

    /**
     * 获取背包编辑服务
     */
    fun getInvEditService(): InvEditService? {
        return if (this::invEditService.isInitialized) invEditService else null
    }

    /**
     * 获取电梯服务
     */
    fun getElevatorService(): ElevatorService? {
        return if (this::elevatorService.isInitialized) elevatorService else null
    }

    /**
     * 获取头部装备服务
     */
    fun getHeadService(): HeadService? {
        return if (this::headService.isInitialized) headService else null
    }


}
