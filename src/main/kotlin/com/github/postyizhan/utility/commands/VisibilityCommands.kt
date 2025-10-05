package com.github.postyizhan.utility.commands

import com.github.postyizhan.PostBits
import com.github.postyizhan.util.MessageUtil
import com.github.postyizhan.utility.events.VanishStatusChangeEvent
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID

/**
 * 可见性命令处理器
 * 参考 Essentials 实现的隐身功能
 * 包括：实体隐藏、隐身药水效果、睡眠忽略等
 * 
 * @author postyizhan
 */
class VisibilityCommands(private val plugin: PostBits) {

    companion object {
        const val VANISH_METADATA_KEY = "postbits_vanished"
    }

    // 存储隐身玩家的名字集合（用于快速查找）
    private val vanishedPlayers = mutableSetOf<String>()
    
    // 记录玩家最后隐身时间
    private val lastVanishTime = mutableMapOf<UUID, Long>()

    init {
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: VisibilityCommands initialized with Essentials-style implementation")
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

        // 切换隐身状态
        val newState = !isVanished(sender)
        
        // 触发事件
        val event = VanishStatusChangeEvent(sender, sender, newState)
        Bukkit.getPluginManager().callEvent(event)
        
        // 如果事件被取消，则不执行
        if (event.isCancelled) {
            return true
        }
        
        // 设置隐身状态
        setVanished(sender, newState)
        
        // 发送消息
        if (newState) {
            MessageUtil.sendMessage(sender, "utility.vanish_enabled")
        } else {
            MessageUtil.sendMessage(sender, "utility.vanish_disabled")
        }

        return true
    }

    /**
     * 设置玩家的隐身状态
     * 
     * @param player 要设置的玩家
     * @param vanished 是否隐身
     */
    fun setVanished(player: Player, vanished: Boolean) {
        if (vanished) {
            // 隐身玩家
            vanishedPlayers.add(player.name)
            lastVanishTime[player.uniqueId] = System.currentTimeMillis()
            player.setMetadata(VANISH_METADATA_KEY, FixedMetadataValue(plugin, true))
            
            // 对所有在线玩家隐藏此玩家
            Bukkit.getOnlinePlayers().forEach { other ->
                if (other != player && !other.hasPermission("postbits.utility.vanish.see")) {
                    @Suppress("DEPRECATION")
                    other.hidePlayer(player)
                }
            }
            
            // 添加隐身药水效果（如果有权限）
            if (player.hasPermission("postbits.utility.vanish.effect")) {
                player.addPotionEffect(
                    PotionEffect(
                        PotionEffectType.INVISIBILITY,
                        Integer.MAX_VALUE,
                        1,
                        false,
                        false
                    )
                )
            }
            
            // 设置睡眠忽略
            if (getSleepIgnoresVanishedPlayers()) {
                player.isSleepingIgnored = true
            }
            
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: Player ${player.name} is now vanished")
            }
        } else {
            // 取消隐身
            vanishedPlayers.remove(player.name)
            lastVanishTime.remove(player.uniqueId)
            player.removeMetadata(VANISH_METADATA_KEY, plugin)
            
            // 对所有在线玩家显示此玩家
            Bukkit.getOnlinePlayers().forEach { other ->
                if (other != player) {
                    @Suppress("DEPRECATION")
                    other.showPlayer(player)
                }
            }
            
            // 移除隐身药水效果
            if (player.hasPermission("postbits.utility.vanish.effect")) {
                player.removePotionEffect(PotionEffectType.INVISIBILITY)
            }
            
            // 恢复睡眠设置
            if (getSleepIgnoresVanishedPlayers() && !player.hasPermission("postbits.utility.sleepingignored")) {
                player.isSleepingIgnored = false
            }
            
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: Player ${player.name} is no longer vanished")
            }
        }
    }

    /**
     * 检查玩家是否隐身
     */
    fun isVanished(player: Player): Boolean {
        return player.hasMetadata(VANISH_METADATA_KEY) &&
               player.getMetadata(VANISH_METADATA_KEY).firstOrNull()?.asBoolean() == true
    }

    /**
     * 获取所有隐身玩家的名字集合
     */
    fun getVanishedPlayers(): Set<String> {
        return vanishedPlayers.toSet()
    }

    /**
     * 获取玩家最后隐身的时间
     */
    fun getLastVanishTime(player: Player): Long? {
        return lastVanishTime[player.uniqueId]
    }

    /**
     * 玩家加入时处理隐身玩家可见性
     */
    fun onPlayerJoin(player: Player) {
        // 对新加入的玩家隐藏所有已隐身的玩家
        if (!player.hasPermission("postbits.utility.vanish.see")) {
            vanishedPlayers.forEach { vanishedName ->
                val vanishedPlayer = Bukkit.getPlayerExact(vanishedName)
                if (vanishedPlayer != null && vanishedPlayer.isOnline) {
                    @Suppress("DEPRECATION")
                    player.hidePlayer(vanishedPlayer)
                    
                    if (plugin.isDebugEnabled()) {
                        plugin.logger.info("Debug: Hiding vanished player $vanishedName from ${player.name}")
                    }
                }
            }
        }
        
        // 如果加入的玩家本身是隐身的，重新应用隐身状态
        if (isVanished(player)) {
            Bukkit.getOnlinePlayers().forEach { other ->
                if (other != player && !other.hasPermission("postbits.utility.vanish.see")) {
                    @Suppress("DEPRECATION")
                    other.hidePlayer(player)
                }
            }
            
            // 确保隐身玩家在列表中
            if (!vanishedPlayers.contains(player.name)) {
                vanishedPlayers.add(player.name)
            }
        }
        
        // 支持自动隐身（如果有权限）
        if (player.hasPermission("postbits.utility.vanish.onjoin")) {
            setVanished(player, true)
            MessageUtil.sendMessage(player, "utility.vanish_auto_enabled")
            
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: Auto-vanished ${player.name} on join")
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
        // 只清理内存数据，保持元数据以便下次登录恢复
        if (isVanished(player)) {
            // 注意：这里不从 vanishedPlayers 中移除，因为玩家可能重新登录
            // 但我们需要在一段时间后清理离线玩家
            
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: Player ${player.name} quit while vanished")
            }
        }
    }

    /**
     * 玩家改变世界时检查隐身权限
     */
    fun onPlayerChangeWorld(player: Player) {
        if (isVanished(player)) {
            // 检查玩家是否还有隐身权限
            if (!player.hasPermission("postbits.utility.vanish")) {
                setVanished(player, false)
                MessageUtil.sendMessage(player, "utility.vanish_permission_lost")
                
                if (plugin.isDebugEnabled()) {
                    plugin.logger.info("Debug: ${player.name} lost vanish permission in new world")
                }
            } else {
                // 重新应用隐身状态
                setVanished(player, true)
            }
        }
    }

    /**
     * 清理所有隐身玩家（插件禁用时）
     */
    fun cleanup() {
        // 取消所有在线玩家的隐身状态
        vanishedPlayers.toList().forEach { playerName ->
            val player = Bukkit.getPlayerExact(playerName)
            if (player != null && player.isOnline) {
                setVanished(player, false)
                
                if (plugin.isDebugEnabled()) {
                    plugin.logger.info("Debug: Unvanished player $playerName during cleanup")
                }
            }
        }
        
        vanishedPlayers.clear()
        lastVanishTime.clear()
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: All vanish states cleared")
        }
    }

    /**
     * 定期清理离线玩家的隐身记录
     */
    fun cleanupOfflinePlayers() {
        val toRemove = mutableListOf<String>()
        
        vanishedPlayers.forEach { playerName ->
            val player = Bukkit.getPlayerExact(playerName)
            if (player == null || !player.isOnline) {
                toRemove.add(playerName)
            }
        }
        
        toRemove.forEach { playerName ->
            vanishedPlayers.remove(playerName)
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: Removed offline vanished player: $playerName")
            }
        }
    }

    /**
     * 从配置获取是否启用睡眠忽略隐身玩家
     */
    private fun getSleepIgnoresVanishedPlayers(): Boolean {
        return plugin.getConfigManager().getConfig()
            .getBoolean("modules.utility.vanish.sleep-ignores-vanished-players", true)
    }
}
