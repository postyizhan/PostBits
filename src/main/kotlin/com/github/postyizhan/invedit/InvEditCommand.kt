package com.github.postyizhan.invedit

import com.github.postyizhan.PostBits
import com.github.postyizhan.util.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * 背包编辑命令处理器
 * 处理背包编辑相关的子命令
 *
 * @author postyizhan
 */
class InvEditCommand(private val plugin: PostBits, private val invEditService: InvEditService) {
    
    /**
     * 执行背包编辑命令
     */
    fun execute(sender: CommandSender, args: Array<String>): Boolean {
        if (sender !is Player) {
            MessageUtil.sendMessage(sender, "messages.player_only")
            return true
        }
        
        // 检查权限
        if (!sender.hasPermission("postbits.invedit.use")) {
            MessageUtil.sendMessage(sender, "messages.no_permission")
            return true
        }
        
        // 处理子命令
        when {
            args.isEmpty() -> {
                MessageUtil.sendMessage(sender, "invedit.invalid_usage")
                return true
            }
            
            args[0].equals("reload", ignoreCase = true) -> {
                if (!sender.hasPermission("postbits.admin.reload")) {
                    MessageUtil.sendMessage(sender, "messages.no_permission")
                    return true
                }
                
                plugin.reload()
                MessageUtil.sendMessage(sender, "messages.reload")
                return true
            }
            
            else -> {
                // 尝试作为玩家名处理
                return handlePlayerEdit(sender, args[0])
            }
        }
    }
    
    /**
     * 处理编辑玩家背包
     */
    private fun handlePlayerEdit(editor: Player, targetName: String): Boolean {
        // 查找目标玩家
        val target = Bukkit.getPlayer(targetName)
        
        if (target == null) {
            MessageUtil.sendMessage(editor, "invedit.target_not_found")
            return true
        }
        
        // 打开背包编辑界面
        invEditService.openPlayerInventory(editor, target)
        
        return true
    }
    
    /**
     * 提供命令补全建议
     */
    fun onTabComplete(sender: CommandSender, args: Array<String>): List<String> {
        val completions = mutableListOf<String>()
        
        if (args.size == 1) {
            val input = args[0].lowercase()
            
            // 添加子命令
            if (sender.hasPermission("postbits.admin.reload") && "reload".startsWith(input)) {
                completions.add("reload")
            }
            
            // 添加在线玩家名
            completions.addAll(
                Bukkit.getOnlinePlayers()
                    .map { it.name }
                    .filter { it.lowercase().startsWith(input) }
            )
        }
        
        return completions.sorted()
    }
}

