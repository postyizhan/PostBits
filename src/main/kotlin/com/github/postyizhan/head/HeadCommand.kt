package com.github.postyizhan.head

import com.github.postyizhan.PostBits
import com.github.postyizhan.util.MessageUtil
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * 头部装备指令处理器
 * 
 * @author postyizhan
 */
class HeadCommand(private val plugin: PostBits, private val headService: HeadService) {

    fun execute(sender: CommandSender, args: Array<String>): Boolean {
        if (sender !is Player) {
            MessageUtil.sendMessage(sender, "messages.player_only")
            return true
        }

        if (!sender.hasPermission("postbits.head.use")) {
            MessageUtil.sendMessage(sender, "messages.no_permission")
            return true
        }

        when {
            args.isEmpty() -> {
                headService.wearItemOnHead(sender)
            }
            
            args[0].equals("remove", ignoreCase = true) -> {
                headService.removeHeadItem(sender)
            }
            
            else -> {
                MessageUtil.sendMessage(sender, "head.help")
            }
        }
        return true
    }

    fun onTabComplete(@Suppress("UNUSED_PARAMETER") sender: CommandSender, args: Array<String>): List<String> {
        if (args.size == 1 && "remove".startsWith(args[0].lowercase())) {
            return listOf("remove")
        }
        return emptyList()
    }
}
