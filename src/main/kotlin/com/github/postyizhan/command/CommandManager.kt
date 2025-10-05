package com.github.postyizhan.command

import com.github.postyizhan.PostBits
import com.github.postyizhan.chair.ChairCommand
import com.github.postyizhan.utility.UtilityCommand
import com.github.postyizhan.invedit.InvEditCommand
import com.github.postyizhan.portabletools.PortableToolsCommand
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

    private val utilityCommand: UtilityCommand? by lazy {
        plugin.getUtilityService()?.let { UtilityCommand(plugin, it) }
    }

    private val portableToolsCommand: PortableToolsCommand? by lazy {
        plugin.getPortableToolsService()?.let { PortableToolsCommand(plugin, it) }
    }


    /**
     * 处理命令执行
     */
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        when (command.name.lowercase()) {
            "postbits" -> return handleMainCommand(sender, args)
            "heal" -> return handleUtilityCommand("heal", sender, args)
            "suicide" -> return handleUtilityCommand("suicide", sender, args)
            "fix" -> return handleUtilityCommand("fix", sender, args)
            "hat" -> return handleUtilityCommand("hat", sender, args)
            "speed" -> return handleUtilityCommand("speed", sender, args)
            "fly" -> return handleUtilityCommand("fly", sender, args)
            "vanish" -> return handleUtilityCommand("vanish", sender, args)
            "toast" -> return handleUtilityCommand("toast", sender, args)
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
            "craft", "grindstone", "cartography", "enchanting", "smithing", "enderchest" -> {
                return handlePortableToolsCommand(sender, args)
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
     * 处理实用命令
     */
    private fun handleUtilityCommand(commandType: String, sender: CommandSender, args: Array<out String>): Boolean {
        val utilityCmd = utilityCommand
        if (utilityCmd == null) {
            MessageUtil.sendMessage(sender, "messages.module_disabled")
            return true
        }
        
        return when (commandType) {
            "heal" -> utilityCmd.handleHeal(sender, arrayOf(*args))
            "suicide" -> utilityCmd.handleSuicide(sender, arrayOf(*args))
            "fix" -> utilityCmd.handleFix(sender, arrayOf(*args))
            "hat" -> utilityCmd.handleHat(sender, arrayOf(*args))
            "speed" -> utilityCmd.handleSpeed(sender, arrayOf(*args))
            "fly" -> utilityCmd.handleFly(sender, arrayOf(*args))
            "vanish" -> utilityCmd.handleVanish(sender, arrayOf(*args))
            "toast" -> utilityCmd.handleToast(sender, arrayOf(*args))
            else -> false
        }
    }

    /**
     * 处理随身工具命令
     */
    private fun handlePortableToolsCommand(sender: CommandSender, args: Array<out String>): Boolean {
        val portableToolsCmd = portableToolsCommand
        if (portableToolsCmd == null) {
            MessageUtil.sendMessage(sender, "messages.module_disabled")
            return true
        }
        return portableToolsCmd.execute(sender, arrayOf(*args))
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

        // 只有在实用命令模块启用时才显示实用命令
        if (plugin.getConfigManager().getConfig().getBoolean("modules.utility.enabled", false)) {
            MessageUtil.sendMessage(sender, "commands.help.utility")
        }

        // 只有在随身工具模块启用时才显示随身工具命令
        if (plugin.getConfigManager().getConfig().getBoolean("modules.portabletools.enabled", false)) {
            MessageUtil.sendMessage(sender, "commands.help.portabletools")
        }



    }

    /**
     * 处理命令补全
     */
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        // 处理独立的实用命令补全
        when (command.name.lowercase()) {
            "heal", "suicide", "fix", "vanish" -> return emptyList()
            "hat" -> return utilityCommand?.onTabComplete("hat", args) ?: emptyList()
            "speed" -> return utilityCommand?.onTabComplete("speed", args) ?: emptyList()
            "fly" -> return utilityCommand?.onTabComplete("fly", args) ?: emptyList()
            "toast" -> return utilityCommand?.onTabComplete("toast", args) ?: emptyList()
        }
        
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


            // 随身工具命令（仅在模块启用时显示）
            if (plugin.getConfigManager().getConfig().getBoolean("modules.portabletools.enabled", false)) {
                if (sender.hasPermission("postbits.portabletools.craft")) {
                    completions.add("craft")
                }
                if (sender.hasPermission("postbits.portabletools.grindstone")) {
                    completions.add("grindstone")
                }
                if (sender.hasPermission("postbits.portabletools.cartography")) {
                    completions.add("cartography")
                }
                if (sender.hasPermission("postbits.portabletools.enchanting")) {
                    completions.add("enchanting")
                }
                if (sender.hasPermission("postbits.portabletools.smithing")) {
                    completions.add("smithing")
                }
                if (sender.hasPermission("postbits.portabletools.enderchest")) {
                    completions.add("enderchest")
                }
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
                "craft", "grindstone", "cartography", "enchanting", "smithing", "enderchest" -> {
                    val portableToolsCmd = portableToolsCommand
                    if (portableToolsCmd != null) {
                        return portableToolsCmd.onTabComplete(sender, args.drop(1).toTypedArray())
                    }
                }
            }
        }

        return emptyList()
    }
}
