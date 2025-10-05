package com.github.postyizhan.utility.events

import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * 隐身状态改变事件
 * 当玩家的隐身状态因 /vanish 命令改变时触发
 * 
 * @param player 状态改变的玩家
 * @param controller 执行命令的玩家（可能是自己或管理员）
 * @param isVanished 新的隐身状态
 * 
 * @author postyizhan
 */
class VanishStatusChangeEvent(
    val player: Player,
    val controller: Player?,
    val isVanished: Boolean
) : Event(), Cancellable {

    private var cancelled = false

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }

    override fun getHandlers(): HandlerList = HANDLERS

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }
}
