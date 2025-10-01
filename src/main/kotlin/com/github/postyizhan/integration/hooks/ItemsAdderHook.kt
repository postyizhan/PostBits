package com.github.postyizhan.integration.hooks

import com.github.postyizhan.integration.BlockProvider
import com.github.postyizhan.integration.PluginHook
import org.bukkit.Bukkit
import org.bukkit.block.Block
import org.bukkit.plugin.Plugin

/**
 * ItemsAdder 插件挂钩
 * 提供对 ItemsAdder 自定义方块的支持
 * 
 * @author postyizhan
 */
class ItemsAdderHook : PluginHook {
    
    override val pluginName: String = "ItemsAdder"
    override val priority: Int = 15
    
    private var enabled = false
    private var blockProvider: ItemsAdderBlockProvider? = null
    private var plugin: Plugin? = null
    
    override fun initialize(plugin: Plugin): Boolean {
        this.plugin = plugin
        try {
            // 检查 ItemsAdder 是否已加载
            val itemsAdder = Bukkit.getPluginManager().getPlugin("ItemsAdder")
            if (itemsAdder == null || !itemsAdder.isEnabled) {
                plugin.logger.info("[ItemsAdder] Plugin not found or not enabled")
                return false
            }
            
            // 尝试加载 ItemsAdder API 类
            val apiClass = Class.forName("dev.lone.itemsadder.api.CustomBlock")
            
            // 创建方块提供者
            blockProvider = ItemsAdderBlockProvider(apiClass, plugin)
            enabled = true
            
            plugin.logger.info("[ItemsAdder] Successfully integrated (version: ${itemsAdder.description.version})")
            return true
            
        } catch (e: ClassNotFoundException) {
            plugin.logger.warning("[ItemsAdder] API class not found, version incompatible")
            if (plugin.config.getBoolean("debug", false)) {
                e.printStackTrace()
            }
            return false
        } catch (e: Exception) {
            plugin.logger.warning("[ItemsAdder] Initialization failed: ${e.message}")
            if (plugin.config.getBoolean("debug", false)) {
                e.printStackTrace()
            }
            return false
        }
    }
    
    override fun isEnabled(): Boolean = enabled
    
    override fun unload() {
        enabled = false
        blockProvider = null
    }
    
    override fun getBlockProvider(): BlockProvider? = blockProvider
}

/**
 * ItemsAdder 方块提供者
 */
class ItemsAdderBlockProvider(private val apiClass: Class<*>, private val plugin: Plugin) : BlockProvider {
    
    override val providerName: String = "ItemsAdder"
    private val debugEnabled: Boolean
        get() = plugin.config.getBoolean("debug", false)
    
    override fun isCustomBlock(block: Block): Boolean {
        return try {
            // CustomBlock.byAlreadyPlaced(block) != null
            val method = apiClass.getMethod("byAlreadyPlaced", Block::class.java)
            val customBlock = method.invoke(null, block)
            val result = customBlock != null
            
            if (debugEnabled && result) {
                plugin.logger.info("Debug: [ItemsAdder] Block at ${block.location} is a custom block")
            }
            
            result
        } catch (e: Exception) {
            if (debugEnabled) {
                plugin.logger.warning("Debug: [ItemsAdder] Error checking if block is custom: ${e.message}")
                e.printStackTrace()
            }
            false
        }
    }
    
    override fun getBlockId(block: Block): String? {
        return try {
            val byPlacedMethod = apiClass.getMethod("byAlreadyPlaced", Block::class.java)
            val customBlock = byPlacedMethod.invoke(null, block)
            
            if (customBlock == null) {
                if (debugEnabled) {
                    plugin.logger.info("Debug: [ItemsAdder] Custom block is null for ${block.type} at ${block.location}")
                }
                return null
            }
            
            // customBlock.getNamespacedID()
            val getIdMethod = customBlock.javaClass.getMethod("getNamespacedID")
            val blockId = getIdMethod.invoke(customBlock) as? String
            
            if (debugEnabled) {
                plugin.logger.info("Debug: [ItemsAdder] Got block ID: $blockId")
            }
            
            blockId
        } catch (e: Exception) {
            if (debugEnabled) {
                plugin.logger.warning("Debug: [ItemsAdder] Error getting block ID: ${e.message}")
                e.printStackTrace()
            }
            null
        }
    }
    
    override fun getBlockDisplayName(block: Block): String? {
        return try {
            val byPlacedMethod = apiClass.getMethod("byAlreadyPlaced", Block::class.java)
            val customBlock = byPlacedMethod.invoke(null, block) ?: return null
            
            // customBlock.getDisplayName()
            val getNameMethod = customBlock.javaClass.getMethod("getDisplayName")
            getNameMethod.invoke(customBlock) as? String
        } catch (e: Exception) {
            if (debugEnabled) {
                plugin.logger.warning("Debug: [ItemsAdder] Error getting display name: ${e.message}")
            }
            null
        }
    }
}
