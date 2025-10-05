package com.github.postyizhan.utility.commands

import com.github.postyizhan.PostBits
import com.github.postyizhan.util.MessageUtil
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * 玩家状态命令处理器
 * 包含：/heal, /suicide
 * 
 * @author postyizhan
 */
class PlayerStateCommands(private val plugin: PostBits) {

    /**
     * 处理 /heal 命令
     */
    fun handleHeal(sender: CommandSender, @Suppress("UNUSED_PARAMETER") args: Array<String>): Boolean {
        if (sender !is Player) {
            MessageUtil.sendMessage(sender, "messages.player_only")
            return true
        }

        if (!sender.hasPermission("postbits.utility.heal")) {
            MessageUtil.sendMessage(sender, "messages.no_permission")
            return true
        }

        // 恢复生命值
        @Suppress("DEPRECATION")
        sender.health = sender.maxHealth
        
        // 恢复饱食度
        sender.foodLevel = 20
        
        // 恢复饱和度
        sender.saturation = 20f
        
        // 清除着火状态
        sender.fireTicks = 0
        
        MessageUtil.sendMessage(sender, "utility.heal_success")
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: Player ${sender.name} healed")
        }
        
        return true
    }

    /**
     * 处理 /suicide 命令
     */
    fun handleSuicide(sender: CommandSender, @Suppress("UNUSED_PARAMETER") args: Array<String>): Boolean {
        if (sender !is Player) {
            MessageUtil.sendMessage(sender, "messages.player_only")
            return true
        }

        if (!sender.hasPermission("postbits.utility.suicide")) {
            MessageUtil.sendMessage(sender, "messages.no_permission")
            return true
        }

        // 记录自杀前的状态（用于调试）
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: Player ${sender.name} committed suicide at ${sender.location}")
        }
        
        // 设置生命值为 0（触发死亡）
        sender.health = 0.0
        
        // 不需要发送消息，因为玩家会看到死亡屏幕
        
        return true
    }
}
