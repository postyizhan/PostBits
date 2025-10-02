package com.github.postyizhan.utility.commands

import com.github.postyizhan.PostBits
import com.github.postyizhan.util.MessageUtil
import org.bukkit.GameMode
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * 移动命令处理器
 * 包含：/speed, /fly
 * 
 * @author postyizhan
 */
class MovementCommands(private val plugin: PostBits) {

    /**
     * 处理 /speed 命令
     */
    fun handleSpeed(sender: CommandSender, args: Array<String>): Boolean {
        if (sender !is Player) {
            MessageUtil.sendMessage(sender, "messages.player_only")
            return true
        }

        if (!sender.hasPermission("postbits.utility.speed")) {
            MessageUtil.sendMessage(sender, "messages.no_permission")
            return true
        }

        when {
            args.isEmpty() -> {
                MessageUtil.sendMessage(sender, "utility.speed_help")
            }
            
            args[0].equals("reset", ignoreCase = true) -> {
                resetPlayerSpeed(sender)
            }
            
            else -> {
                // 尝试解析速度值
                val speed = args[0].toIntOrNull()
                if (speed == null) {
                    MessageUtil.sendMessage(sender, "utility.speed_invalid_number")
                    return true
                }
                
                // 检查是否指定了 fly/walk
                val flying = args.size > 1 && args[1].equals("fly", ignoreCase = true)
                
                setPlayerSpeed(sender, speed, flying)
            }
        }
        return true
    }

    /**
     * 处理 /fly 命令
     */
    fun handleFly(sender: CommandSender, args: Array<String>): Boolean {
        if (sender !is Player) {
            MessageUtil.sendMessage(sender, "messages.player_only")
            return true
        }

        if (!sender.hasPermission("postbits.utility.fly")) {
            MessageUtil.sendMessage(sender, "messages.no_permission")
            return true
        }

        when {
            args.isEmpty() -> {
                toggleFly(sender)
            }
            
            args[0].equals("on", ignoreCase = true) || args[0].equals("enable", ignoreCase = true) -> {
                setFly(sender, true)
            }
            
            args[0].equals("off", ignoreCase = true) || args[0].equals("disable", ignoreCase = true) -> {
                setFly(sender, false)
            }
            
            else -> {
                MessageUtil.sendMessage(sender, "utility.fly_help")
            }
        }
        return true
    }

    /**
     * 设置玩家速度
     */
    private fun setPlayerSpeed(player: Player, speed: Int, flying: Boolean): Boolean {
        // 验证速度范围
        if (speed !in 1..10) {
            MessageUtil.sendMessage(player, "utility.speed_invalid_range")
            return false
        }
        
        // 转换为 Minecraft 的速度值 (-1.0 到 1.0，正常速度是 0.2)
        // 速度 1 = 0.1, 速度 5 = 0.5, 速度 10 = 1.0
        val minecraftSpeed = speed / 10.0f
        
        if (flying) {
            player.flySpeed = minecraftSpeed
            MessageUtil.sendMessage(player, "utility.speed_fly_success", 
                "{speed}" to speed.toString())
        } else {
            player.walkSpeed = minecraftSpeed
            MessageUtil.sendMessage(player, "utility.speed_walk_success", 
                "{speed}" to speed.toString())
        }
        
        if (plugin.isDebugEnabled()) {
            val type = if (flying) "fly" else "walk"
            plugin.logger.info("Debug: Player ${player.name} set $type speed to $speed")
        }
        
        return true
    }

    /**
     * 重置玩家速度为默认值
     */
    private fun resetPlayerSpeed(player: Player): Boolean {
        player.walkSpeed = 0.2f  // 默认行走速度
        player.flySpeed = 0.1f   // 默认飞行速度
        
        MessageUtil.sendMessage(player, "utility.speed_reset")
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: Player ${player.name} speed reset to default")
        }
        
        return true
    }

    /**
     * 切换玩家飞行模式
     */
    private fun toggleFly(player: Player): Boolean {
        // 创造模式下不需要切换（本来就能飞）
        if (player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR) {
            MessageUtil.sendMessage(player, "utility.fly_gamemode_conflict")
            return false
        }
        
        val newState = !player.allowFlight
        player.allowFlight = newState
        
        // 如果禁用飞行，确保玩家落地
        if (!newState && player.isFlying) {
            player.isFlying = false
        }
        
        if (newState) {
            MessageUtil.sendMessage(player, "utility.fly_enabled")
        } else {
            MessageUtil.sendMessage(player, "utility.fly_disabled")
        }
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: Player ${player.name} fly mode ${if (newState) "enabled" else "disabled"}")
        }
        
        return true
    }

    /**
     * 设置玩家飞行模式
     */
    private fun setFly(player: Player, enabled: Boolean): Boolean {
        // 创造模式下不需要设置
        if (player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR) {
            MessageUtil.sendMessage(player, "utility.fly_gamemode_conflict")
            return false
        }
        
        player.allowFlight = enabled
        
        // 如果禁用飞行，确保玩家落地
        if (!enabled && player.isFlying) {
            player.isFlying = false
        }
        
        if (enabled) {
            MessageUtil.sendMessage(player, "utility.fly_enabled")
        } else {
            MessageUtil.sendMessage(player, "utility.fly_disabled")
        }
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: Player ${player.name} fly mode set to $enabled")
        }
        
        return true
    }

    /**
     * Tab 补全 - Speed 命令
     */
    fun onTabCompleteSpeed(args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> {
                val suggestions = mutableListOf("reset")
                suggestions.addAll((1..10).map { it.toString() })
                suggestions.filter { it.startsWith(args[0].lowercase()) }
            }
            2 -> {
                listOf("walk", "fly").filter { it.startsWith(args[1].lowercase()) }
            }
            else -> emptyList()
        }
    }

    /**
     * Tab 补全 - Fly 命令
     */
    fun onTabCompleteFly(args: Array<out String>): List<String> {
        if (args.size == 1) {
            return listOf("on", "off", "enable", "disable").filter { it.startsWith(args[0].lowercase()) }
        }
        return emptyList()
    }
}


