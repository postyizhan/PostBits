package com.github.postyizhan.elevator

import com.github.postyizhan.PostBits
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerToggleSneakEvent

/**
 * 电梯功能事件监听器
 * 处理电梯相关的所有交互事件
 *
 * @author postyizhan
 */
class ElevatorEventHandler(
    private val plugin: PostBits,
    private val elevatorService: ElevatorService
) : Listener {
    
    /**
     * 处理玩家移动事件 - 检测跳跃进行电梯上升
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        
        // 检查权限
        if (!player.hasPermission("postbits.elevator.use")) {
            return
        }
        
        // 检查是否在跳跃（Y轴向上移动且玩家在地面上）
        val from = event.from
        val to = event.to ?: return
        
        if (from.y < to.y && 
            !player.isFlying && 
            to.y - from.y > 0.1) {
            
            // 处理电梯上升
            if (elevatorService.handleElevatorUp(player)) {
                if (plugin.isDebugEnabled()) {
                    plugin.logger.info("Debug: ${player.name} triggered elevator up")
                }
            }
        }
    }
    
    /**
     * 处理玩家蹲下事件 - 电梯下降
     */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerToggleSneak(event: PlayerToggleSneakEvent) {
        val player = event.player
        
        // 只在开始蹲下时处理
        if (!event.isSneaking) {
            return
        }
        
        // 检查权限
        if (!player.hasPermission("postbits.elevator.use")) {
            return
        }
        
        // 处理电梯下降
        if (elevatorService.handleElevatorDown(player)) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: ${player.name} triggered elevator down")
            }
        }
    }
    
    /**
     * 处理玩家退出事件
     */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        elevatorService.onPlayerQuit(event.player)
    }
}
