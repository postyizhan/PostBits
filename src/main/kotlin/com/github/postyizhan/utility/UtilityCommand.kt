package com.github.postyizhan.utility

import com.github.postyizhan.PostBits
import org.bukkit.command.CommandSender

/**
 * 实用命令委托器
 * 将命令请求分发到对应的命令处理器
 * 
 * @author postyizhan
 */
class UtilityCommand(private val plugin: PostBits, private val utilityService: UtilityService) {

    // ==================== 命令处理 ====================

    fun handleHeal(sender: CommandSender, args: Array<String>): Boolean {
        return utilityService.playerStateCommands.handleHeal(sender, args)
    }

    fun handleSuicide(sender: CommandSender, args: Array<String>): Boolean {
        return utilityService.playerStateCommands.handleSuicide(sender, args)
    }

    fun handleFix(sender: CommandSender, args: Array<String>): Boolean {
        return utilityService.itemCommands.handleFix(sender, args)
    }

    fun handleHat(sender: CommandSender, args: Array<String>): Boolean {
        return utilityService.itemCommands.handleHat(sender, args)
    }

    fun handleSpeed(sender: CommandSender, args: Array<String>): Boolean {
        return utilityService.movementCommands.handleSpeed(sender, args)
    }

    fun handleFly(sender: CommandSender, args: Array<String>): Boolean {
        return utilityService.movementCommands.handleFly(sender, args)
    }

    fun handleVanish(sender: CommandSender, args: Array<String>): Boolean {
        return utilityService.visibilityCommands.handleVanish(sender, args)
    }

    // ==================== Tab 补全 ====================

    fun onTabComplete(command: String, args: Array<out String>): List<String> {
        return when (command.lowercase()) {
            "hat" -> utilityService.itemCommands.onTabComplete(args)
            "speed" -> utilityService.movementCommands.onTabCompleteSpeed(args)
            "fly" -> utilityService.movementCommands.onTabCompleteFly(args)
            else -> emptyList()
        }
    }
}
