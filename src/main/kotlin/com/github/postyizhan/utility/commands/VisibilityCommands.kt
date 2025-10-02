package com.github.postyizhan.utility.commands

import com.github.postyizhan.PostBits
import com.github.postyizhan.util.MessageUtil
import com.github.postyizhan.util.hook.HookManager
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.metadata.FixedMetadataValue
import java.util.UUID

/**
 * 可见性命令处理器
 * 使用 Bukkit API 的 hidePlayer/showPlayer 实现隐身功能
 * 需要 ProtocolLib 作为依赖检查
 * 
 * @author postyizhan
 */
class VisibilityCommands(private val plugin: PostBits, private val hookManager: HookManager) {

    companion object {
        const val VANISH_METADATA_KEY = "postbits_vanished"
    }

    // 存储隐身玩家的 UUID
    private val vanishedPlayers = mutableSetOf<UUID>()
    
    private val protocolLibAvailable: Boolean
        get() = hookManager.isEnabled("ProtocolLib")

    init {
        if (!protocolLibAvailable) {
            plugin.logger.warning("ProtocolLib not available! /vanish command will be disabled.")
        } else {
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: VisibilityCommands initialized with ProtocolLib support")
            }
        }
    }

    /**
     * 处理 /vanish 命令
     */
    fun handleVanish(sender: CommandSender, @Suppress("UNUSED_PARAMETER") args: Array<String>): Boolean {
        if (sender !is Player) {
            MessageUtil.sendMessage(sender, "messages.player_only")
            return true
        }

        if (!sender.hasPermission("postbits.utility.vanish")) {
            MessageUtil.sendMessage(sender, "messages.no_permission")
            return true
        }

        // 检查 ProtocolLib 是否可用
        if (!protocolLibAvailable) {
            MessageUtil.sendMessage(sender, "utility.vanish_protocollib_required")
            return true
        }

        if (isVanished(sender)) {
            unvanish(sender)
            MessageUtil.sendMessage(sender, "utility.vanish_disabled")
        } else {
            vanish(sender)
            MessageUtil.sendMessage(sender, "utility.vanish_enabled")
        }

        return true
    }

    /**
     * 隐身玩家
     */
    private fun vanish(player: Player) {
        if (isVanished(player)) return
        
        vanishedPlayers.add(player.uniqueId)
        player.setMetadata(VANISH_METADATA_KEY, FixedMetadataValue(plugin, true))
        
        // 使用 Bukkit API 隐藏玩家
        Bukkit.getOnlinePlayers().forEach { other ->
            if (other != player && !other.hasPermission("postbits.utility.vanish.see")) {
                other.hidePlayer(plugin, player)
            }
        }
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: Player ${player.name} is now vanished (UUID: ${player.uniqueId})")
        }
    }

    /**
     * 取消隐身
     */
    private fun unvanish(player: Player) {
        if (!isVanished(player)) return
        
        vanishedPlayers.remove(player.uniqueId)
        player.removeMetadata(VANISH_METADATA_KEY, plugin)
        
        // 使用 Bukkit API 显示玩家
        Bukkit.getOnlinePlayers().forEach { other ->
            if (other != player) {
                other.showPlayer(plugin, player)
            }
        }
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: Player ${player.name} is no longer vanished")
        }
    }

    /**
     * 检查玩家是否隐身
     */
    fun isVanished(player: Player): Boolean {
        return player.hasMetadata(VANISH_METADATA_KEY)
    }

    /**
     * 玩家加入时处理隐身玩家可见性
     */
    fun onPlayerJoin(player: Player) {
        if (!protocolLibAvailable) return
        
        // 对新加入的玩家隐藏所有已隐身的玩家
        vanishedPlayers.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { vanished ->
                if (!player.hasPermission("postbits.utility.vanish.see")) {
                    player.hidePlayer(plugin, vanished)
                }
            }
        }
        
        // 如果加入的玩家本身是隐身的，对其他玩家隐藏他
        if (isVanished(player)) {
            Bukkit.getOnlinePlayers().forEach { other ->
                if (other != player && !other.hasPermission("postbits.utility.vanish.see")) {
                    other.hidePlayer(plugin, player)
                }
            }
        }
        
        if (plugin.isDebugEnabled() && vanishedPlayers.isNotEmpty()) {
            plugin.logger.info("Debug: Player ${player.name} joined, processed ${vanishedPlayers.size} vanished players")
        }
    }

    /**
     * 玩家退出时清理隐身状态
     */
    fun onPlayerQuit(player: Player) {
        // 只清理元数据和集合
        // 玩家退出后，其他玩家会自动看不到他
        if (isVanished(player)) {
            vanishedPlayers.remove(player.uniqueId)
            player.removeMetadata(VANISH_METADATA_KEY, plugin)
            
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: Player ${player.name} quit while vanished, cleaned up metadata")
            }
        }
    }

    /**
     * 清理所有隐身玩家（插件禁用时）
     */
    fun cleanup() {
        if (!protocolLibAvailable) {
            vanishedPlayers.clear()
            return
        }
        
        // 取消所有玩家的隐身状态
        vanishedPlayers.toList().forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { player ->
                if (player.isOnline) {
                    unvanish(player)
                    if (plugin.isDebugEnabled()) {
                        plugin.logger.info("Debug: Unvanished player ${player.name} during cleanup")
                    }
                }
            }
        }
        vanishedPlayers.clear()
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: All vanish states cleared")
        }
    }
}
