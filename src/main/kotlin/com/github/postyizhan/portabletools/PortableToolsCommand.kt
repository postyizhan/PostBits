package com.github.postyizhan.portabletools

import com.github.postyizhan.PostBits
import com.github.postyizhan.util.MessageUtil
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * 随身工具指令处理器
 * 
 * @author postyizhan
 */
class PortableToolsCommand(private val plugin: PostBits, private val toolsService: PortableToolsService) {

    fun execute(sender: CommandSender, args: Array<String>): Boolean {
        if (sender !is Player) {
            MessageUtil.sendMessage(sender, "messages.player_only")
            return true
        }

        if (args.isEmpty()) {
            MessageUtil.sendMessage(sender, "portabletools.help")
            return true
        }

        val subCommand = args[0].lowercase()

        when (subCommand) {
            "craft", "workbench", "wb" -> {
                if (!sender.hasPermission("postbits.portabletools.craft")) {
                    MessageUtil.sendMessage(sender, "messages.no_permission")
                    return true
                }
                toolsService.openCraft(sender)
            }

            "grindstone", "gs" -> {
                if (!sender.hasPermission("postbits.portabletools.grindstone")) {
                    MessageUtil.sendMessage(sender, "messages.no_permission")
                    return true
                }
                toolsService.openGrindstone(sender)
            }

            "cartography", "ct" -> {
                if (!sender.hasPermission("postbits.portabletools.cartography")) {
                    MessageUtil.sendMessage(sender, "messages.no_permission")
                    return true
                }
                toolsService.openCartography(sender)
            }

            "enchanting", "et" -> {
                if (!sender.hasPermission("postbits.portabletools.enchanting")) {
                    MessageUtil.sendMessage(sender, "messages.no_permission")
                    return true
                }
                toolsService.openEnchanting(sender)
            }

            "smithing", "st" -> {
                if (!sender.hasPermission("postbits.portabletools.smithing")) {
                    MessageUtil.sendMessage(sender, "messages.no_permission")
                    return true
                }
                toolsService.openSmithing(sender)
            }

            "enderchest", "ec" -> {
                if (!sender.hasPermission("postbits.portabletools.enderchest")) {
                    MessageUtil.sendMessage(sender, "messages.no_permission")
                    return true
                }
                toolsService.openEnderChest(sender)
            }

            else -> {
                MessageUtil.sendMessage(sender, "portabletools.help")
            }
        }
        return true
    }

    fun onTabComplete(sender: CommandSender, args: Array<String>): List<String> {
        if (args.size == 1) {
            val completions = mutableListOf<String>()
            val input = args[0].lowercase()

            if (sender.hasPermission("postbits.portabletools.craft") && "craft".startsWith(input)) {
                completions.add("craft")
            }
            if (sender.hasPermission("postbits.portabletools.grindstone") && "grindstone".startsWith(input)) {
                completions.add("grindstone")
            }
            if (sender.hasPermission("postbits.portabletools.cartography") && "cartography".startsWith(input)) {
                completions.add("cartography")
            }
            if (sender.hasPermission("postbits.portabletools.enchanting") && "enchanting".startsWith(input)) {
                completions.add("enchanting")
            }
            if (sender.hasPermission("postbits.portabletools.smithing") && "smithing".startsWith(input)) {
                completions.add("smithing")
            }
            if (sender.hasPermission("postbits.portabletools.enderchest") && "enderchest".startsWith(input)) {
                completions.add("enderchest")
            }

            return completions
        }
        return emptyList()
    }
}
