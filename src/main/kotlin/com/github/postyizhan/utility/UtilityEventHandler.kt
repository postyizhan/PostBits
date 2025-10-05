package com.github.postyizhan.utility

import com.github.postyizhan.PostBits
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * 实用命令事件处理器
 * 主要处理隐身相关的玩家加入/退出/世界切换事件
 * 
 * @author postyizhan
 */
class UtilityEventHandler(
    private val plugin: PostBits,
    private val utilityService: UtilityService
) : Listener {

    /**
     * 玩家加入事件
     * 对新加入的玩家隐藏所有已隐身的玩家
     * 支持自动隐身功能
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        utilityService.visibilityCommands.onPlayerJoin(event.player)
    }

    /**
     * 玩家退出事件
     * 清理退出玩家的隐身状态
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        utilityService.visibilityCommands.onPlayerQuit(event.player)
    }

    /**
     * 玩家切换世界事件
     * 检查玩家在新世界中是否还有隐身权限
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerChangeWorld(event: PlayerChangedWorldEvent) {
        utilityService.visibilityCommands.onPlayerChangeWorld(event.player)
    }
}


