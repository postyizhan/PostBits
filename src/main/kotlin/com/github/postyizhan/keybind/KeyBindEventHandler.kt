package com.github.postyizhan.keybind

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolManager
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.github.postyizhan.PostBits
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent

/**
 * 按键绑定事件处理器
 * 监听玩家的各种按键操作
 *
 * @author postyizhan
 */
class KeyBindEventHandler(
    private val plugin: PostBits,
    private val service: KeyBindService
) : Listener {
    
    private var protocolManager: ProtocolManager? = null
    private var sneakPacketListener: PacketAdapter? = null
    
    init {
        // 初始化 ProtocolLib 监听器（如果可用）
        initProtocolLib()
    }
    
    /**
     * 初始化 ProtocolLib 监听器
     * 用于监听潜行（Shift）事件
     */
    private fun initProtocolLib() {
        val hookManager = plugin.getHookManager()
        if (!hookManager.isEnabled("ProtocolLib")) {
            plugin.logger.warning("[KeyBind] ProtocolLib not found, Shift key detection will not work")
            plugin.logger.warning("[KeyBind] Download: https://www.spigotmc.org/resources/protocollib.1997/")
            return
        }
        
        protocolManager = hookManager.getService("ProtocolLib", ProtocolManager::class.java)
        if (protocolManager == null) {
            plugin.logger.warning("[KeyBind] Failed to get ProtocolManager")
            return
        }
        
        // 保存外部引用
        val postBitsPlugin = this.plugin
        val keyBindService = this.service
        
        // 监听玩家潜行事件
        sneakPacketListener = object : PacketAdapter(
            plugin,
            ListenerPriority.NORMAL,
            PacketType.Play.Client.ENTITY_ACTION
        ) {
            override fun onPacketReceiving(event: PacketEvent) {
                val player = event.player
                val packet = event.packet
                
                try {
                    // 获取动作类型
                    val action = packet.playerActions.read(0)
                    
                    // 只处理开始潜行的动作
                    if (action.name == "START_SNEAKING") {
                        if (postBitsPlugin.isDebugEnabled()) {
                            postBitsPlugin.logger.info("Debug: [KeyBind] Player ${player.name} started sneaking")
                        }
                        
                        // 触发潜行按键事件
                        keyBindService.handleKeyPress(player, KeyType.SNEAK)
                    }
                } catch (e: Exception) {
                    if (postBitsPlugin.isDebugEnabled()) {
                        postBitsPlugin.logger.warning("Debug: [KeyBind] Error processing sneak packet: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
        }
        
        protocolManager?.addPacketListener(sneakPacketListener)
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: [KeyBind] ProtocolLib listener registered")
        }
    }
    
    /**
     * 监听玩家切换副手事件（F 或 Shift + F）
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onSwapHandItems(event: PlayerSwapHandItemsEvent) {
        val player = event.player
        
        // 检查玩家是否在潜行
        if (player.isSneaking) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: [KeyBind] Player ${player.name} swapped hands while sneaking (Shift+F)")
            }
            
            service.handleKeyPress(player, KeyType.SNEAK_SWAP)
        } else {
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: [KeyBind] Player ${player.name} swapped hands (F)")
            }
            
            service.handleKeyPress(player, KeyType.SWAP_HAND)
        }
    }
    
    /**
     * 监听玩家丢弃物品事件（Q）
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onDropItem(event: PlayerDropItemEvent) {
        val player = event.player
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: [KeyBind] Player ${player.name} dropped item")
        }
        
        service.handleKeyPress(player, KeyType.DROP)
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        // 移除 ProtocolLib 监听器
        sneakPacketListener?.let { listener ->
            protocolManager?.removePacketListener(listener)
        }
        sneakPacketListener = null
        protocolManager = null
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: [KeyBind] Event handler cleaned up")
        }
    }
}
