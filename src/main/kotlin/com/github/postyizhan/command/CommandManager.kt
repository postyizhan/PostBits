package com.github.postyizhan.command

import com.github.postyizhan.PostBits
import com.github.postyizhan.chair.ChairCommand
import com.github.postyizhan.invedit.InvEditCommand
import com.github.postyizhan.util.MessageUtil
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

/**
 * 命令管理器 - 负责处理所有插件命令
 * 
 * @author postyizhan
 */
class CommandManager(private val plugin: PostBits) : CommandExecutor, TabCompleter {

    private val chairCommand: ChairCommand? by lazy {
        plugin.getChairService()?.let { ChairCommand(plugin, it) }
    }

    private val invEditCommand: InvEditCommand? by lazy {
        plugin.getInvEditService()?.let { InvEditCommand(plugin, it) }
    }

    /**
     * 处理命令执行
     */
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        when (command.name.lowercase()) {
            "postbits" -> {
                return handleMainCommand(sender, args)
            }
        }
        return false
    }

    /**
     * 处理主命令
     */
    private fun handleMainCommand(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            // 显示帮助信息
            showHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
            "reload" -> {
                return handleReloadCommand(sender)
            }
            "update" -> {
                return handleUpdateCommand(sender)
            }

            "chair" -> {
                return handleChairCommand(sender, args.drop(1).toTypedArray())
            }
            "invedit" -> {
                return handleInvEditCommand(sender, args.drop(1).toTypedArray())
            }
            "help" -> {
                showHelp(sender)
                return true
            }
            else -> {
                MessageUtil.sendMessage(sender, "messages.unknown_command")
                return true
            }
        }
    }

    /**
     * 处理重载命令
     */
    private fun handleReloadCommand(sender: CommandSender): Boolean {
        if (!sender.hasPermission("postbits.admin.reload")) {
            MessageUtil.sendMessage(sender, "messages.no_permission")
            return true
        }

        try {
            plugin.reload()
            MessageUtil.sendMessage(sender, "messages.reload")
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: Plugin reloaded by ${sender.name}")
            }
        } catch (e: Exception) {
            MessageUtil.sendMessage(sender, "messages.reload_failed")
            plugin.logger.severe("Failed to reload plugin: ${e.message}")
            if (plugin.isDebugEnabled()) {
                e.printStackTrace()
            }
        }
        return true
    }

    /**
     * 处理更新检查命令
     */
    private fun handleUpdateCommand(sender: CommandSender): Boolean {
        if (!plugin.getConfigManager().getConfig().getBoolean("modules.update-checker.enabled", false)) {
            MessageUtil.sendMessage(sender, "messages.module_disabled")
            return true
        }

        if (!sender.hasPermission("postbits.admin.update")) {
            MessageUtil.sendMessage(sender, "messages.no_permission")
            return true
        }

        plugin.sendUpdateInfo(sender)
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: Update check requested by ${sender.name}")
        }
        return true
    }

    /**
     * 处理椅子命令
     */
    private fun handleChairCommand(sender: CommandSender, args: Array<out String>): Boolean {
        val chairCmd = chairCommand
        if (chairCmd == null) {
            MessageUtil.sendMessage(sender, "messages.module_disabled")
            return true
        }
        return chairCmd.handleChairCommand(sender, args)
    }

    /**
     * 处理背包编辑命令
     */
    private fun handleInvEditCommand(sender: CommandSender, args: Array<out String>): Boolean {
        val invEditCmd = invEditCommand
        if (invEditCmd == null) {
            MessageUtil.sendMessage(sender, "messages.module_disabled")
            return true
        }
        return invEditCmd.execute(sender, arrayOf(*args))
    }



    /**
     * 显示帮助信息
     */
    private fun showHelp(sender: CommandSender) {
        MessageUtil.sendMessage(sender, "commands.help.header")
        MessageUtil.sendMessage(sender, "commands.help.reload")
        
        // 只有在更新检查模块启用时才显示更新命令
        if (plugin.getConfigManager().getConfig().getBoolean("modules.update-checker.enabled", false)) {
            MessageUtil.sendMessage(sender, "commands.help.update")
        }

        // 只有在椅子模块启用时才显示椅子命令
        if (plugin.getConfigManager().getConfig().getBoolean("modules.chair.enabled", false)) {
            MessageUtil.sendMessage(sender, "commands.help.chair")
        }

        // 只有在背包编辑模块启用时才显示背包编辑命令
        if (plugin.getConfigManager().getConfig().getBoolean("modules.invedit.enabled", false)) {
            MessageUtil.sendMessage(sender, "commands.help.invedit")
        }


    }

    /**
     * 处理命令补全
     */
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        if (command.name.lowercase() != "postbits") {
            return null
        }

        if (args.size == 1) {
            val completions = mutableListOf<String>()
            
            // 基础命令
            if (sender.hasPermission("postbits.admin.reload")) {
                completions.add("reload")
            }
            
            // 更新检查命令（仅在模块启用时显示）
            if (sender.hasPermission("postbits.admin.update") &&
                plugin.getConfigManager().getConfig().getBoolean("modules.update-checker.enabled", false)) {
                completions.add("update")
            }

            // 椅子命令（仅在模块启用时显示）
            if (sender.hasPermission("postbits.chair.sit") &&
                plugin.getConfigManager().getConfig().getBoolean("modules.chair.enabled", false)) {
                completions.add("chair")
            }

            // 背包编辑命令（仅在模块启用时显示）
            if (sender.hasPermission("postbits.invedit.use") &&
                plugin.getConfigManager().getConfig().getBoolean("modules.invedit.enabled", false)) {
                completions.add("invedit")
            }

            completions.add("help")
            
            // 过滤匹配的命令
            return completions.filter { it.startsWith(args[0].lowercase()) }
        }

        // 处理二级命令补全
        if (args.size == 2) {
            when (args[0].lowercase()) {
                "invedit" -> {
                    val invEditCmd = invEditCommand
                    if (invEditCmd != null && sender.hasPermission("postbits.invedit.use")) {
                        return invEditCmd.onTabComplete(sender, args.drop(1).toTypedArray())
                    }
                }
            }
        }

        return emptyList()
    }
}
