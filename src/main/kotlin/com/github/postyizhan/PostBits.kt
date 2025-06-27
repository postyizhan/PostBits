package com.github.postyizhan

import com.github.postyizhan.command.CommandManager
import com.github.postyizhan.config.ConfigManager
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
}
