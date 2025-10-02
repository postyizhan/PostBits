package com.github.postyizhan.util.hook

import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.ProtocolManager
import org.bukkit.plugin.Plugin

/**
 * ProtocolLib 插件挂钩
 * 
 * @author postyizhan
 */
class ProtocolLibHook : PluginHook {
    
    override val pluginName: String = "ProtocolLib"
    override val hookType: HookType = HookType.PROTOCOL
    
    private var protocolManager: ProtocolManager? = null
    private var enabled = false
    
    override fun initialize(plugin: Plugin): Boolean {
        // 检查 ProtocolLib 是否存在
        val protocolLibPlugin = plugin.server.pluginManager.getPlugin("ProtocolLib")
        if (protocolLibPlugin == null || !protocolLibPlugin.isEnabled) {
            plugin.logger.warning("[ProtocolLib] Plugin not found or not enabled!")
            plugin.logger.warning("[ProtocolLib] Some features (like /vanish) will not work.")
            plugin.logger.warning("[ProtocolLib] Download: https://www.spigotmc.org/resources/protocollib.1997/")
            return false
        }
        
        try {
            protocolManager = ProtocolLibrary.getProtocolManager()
            enabled = true
            
            val version = protocolLibPlugin.description.version
            plugin.logger.info("[ProtocolLib] Successfully integrated (version: $version)")
            return true
            
        } catch (e: Exception) {
            plugin.logger.severe("[ProtocolLib] Failed to initialize: ${e.message}")
            if (plugin is com.github.postyizhan.PostBits && plugin.isDebugEnabled()) {
                e.printStackTrace()
            }
            return false
        }
    }
    
    override fun isEnabled(): Boolean {
        return enabled
    }
    
    override fun unload() {
        protocolManager = null
        enabled = false
    }
    
    override fun <T> getService(serviceClass: Class<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return when (serviceClass) {
            ProtocolManager::class.java -> protocolManager as? T
            else -> null
        }
    }
    
    /**
     * 获取 ProtocolManager 实例
     */
    fun getProtocolManager(): ProtocolManager? {
        return protocolManager
    }
}
