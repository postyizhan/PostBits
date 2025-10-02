package com.github.postyizhan.util.hook

import com.github.postyizhan.util.BlockProvider
import org.bukkit.Bukkit
import org.bukkit.block.Block
import org.bukkit.plugin.Plugin

/**
 * Oraxen 插件挂钩
 * 提供对 Oraxen 自定义方块的支持
 * 
 * @author postyizhan
 */
class OraxenHook : PluginHook {
    
    override val pluginName: String = "Oraxen"
    override val hookType: HookType = HookType.CUSTOM_BLOCK
    
    private var enabled = false
    private var blockProvider: OraxenBlockProvider? = null
    private var plugin: Plugin? = null
    
    override fun initialize(plugin: Plugin): Boolean {
        this.plugin = plugin
        try {
            // 检查 Oraxen 是否已加载
            val oraxen = Bukkit.getPluginManager().getPlugin("Oraxen")
            if (oraxen == null || !oraxen.isEnabled) {
                plugin.logger.info("[Oraxen] Plugin not found or not enabled")
                return false
            }
            
            // 尝试加载 Oraxen API 类
            val apiClass = Class.forName("io.th0rgal.oraxen.api.OraxenBlocks")
            
            // 创建方块提供者
            blockProvider = OraxenBlockProvider(apiClass, plugin)
            enabled = true
            
            plugin.logger.info("[Oraxen] Successfully integrated (version: ${oraxen.description.version})")
            return true
            
        } catch (e: ClassNotFoundException) {
            plugin.logger.warning("[Oraxen] API class not found, version incompatible")
            if (plugin.config.getBoolean("debug", false)) {
                e.printStackTrace()
            }
            return false
        } catch (e: Exception) {
            plugin.logger.warning("[Oraxen] Initialization failed: ${e.message}")
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
    
    override fun <T> getService(serviceClass: Class<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return when (serviceClass) {
            BlockProvider::class.java -> blockProvider as? T
            else -> null
        }
    }
}

/**
 * Oraxen 方块提供者
 */
class OraxenBlockProvider(private val apiClass: Class<*>, private val plugin: Plugin) : BlockProvider {
    
    override val providerName: String = "Oraxen"
    private val debugEnabled: Boolean
        get() = plugin.config.getBoolean("debug", false)
    
    override fun isCustomBlock(block: Block): Boolean {
        return try {
            // OraxenBlocks.isOraxenBlock(block)
            val method = apiClass.getMethod("isOraxenBlock", Block::class.java)
            val result = method.invoke(null, block) as? Boolean ?: false
            
            if (debugEnabled && result) {
                plugin.logger.info("Debug: [Oraxen] Block at ${block.location} is a custom block")
            }
            
            result
        } catch (e: Exception) {
            if (debugEnabled) {
                plugin.logger.warning("Debug: [Oraxen] Error checking if block is custom: ${e.message}")
                e.printStackTrace()
            }
            false
        }
    }
    
    override fun getBlockId(block: Block): String? {
        return try {
            // OraxenBlocks.getOraxenBlock(block.getLocation())
            val getBlockMethod = apiClass.getMethod("getOraxenBlock", org.bukkit.Location::class.java)
            val oraxenBlock = getBlockMethod.invoke(null, block.location)
            
            if (oraxenBlock == null) {
                if (debugEnabled) {
                    plugin.logger.info("Debug: [Oraxen] Mechanic is null for ${block.type} at ${block.location}")
                }
                return null
            }
            
            // mechanic.getItemID()
            val getIdMethod = oraxenBlock.javaClass.getMethod("getItemID")
            val itemId = getIdMethod.invoke(oraxenBlock) as? String
            
            if (itemId == null) {
                if (debugEnabled) {
                    plugin.logger.info("Debug: [Oraxen] ItemID is null for mechanic at ${block.location}")
                }
                return null
            }
            
            // Oraxen 通常不使用命名空间，我们添加一个
            val fullId = "oraxen:$itemId"
            
            if (debugEnabled) {
                plugin.logger.info("Debug: [Oraxen] Got block ID: $fullId (original: $itemId)")
            }
            
            fullId
        } catch (e: Exception) {
            if (debugEnabled) {
                plugin.logger.warning("Debug: [Oraxen] Error getting block ID: ${e.message}")
                e.printStackTrace()
            }
            null
        }
    }
    
    override fun getBlockDisplayName(block: Block): String? {
        return try {
            val blockId = getBlockId(block)
            // 移除 "oraxen:" 前缀作为显示名称
            blockId?.substringAfter(":")
        } catch (e: Exception) {
            if (debugEnabled) {
                plugin.logger.warning("Debug: [Oraxen] Error getting display name: ${e.message}")
            }
            null
        }
    }
}
