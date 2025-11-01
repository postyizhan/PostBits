package com.github.postyizhan.util.hook

import com.github.postyizhan.util.BlockProvider
import org.bukkit.Bukkit
import org.bukkit.block.Block
import org.bukkit.plugin.Plugin

/**
 * CraftEngine 插件挂钩
 * 提供对 CraftEngine 自定义方块的支持
 * 
 * @author postyizhan
 */
class CraftEngineHook : PluginHook {
    
    override val pluginName: String = "CraftEngine"
    override val hookType: HookType = HookType.CUSTOM_BLOCK
    
    private var enabled = false
    private var blockProvider: CraftEngineBlockProvider? = null
    private var plugin: Plugin? = null
    
    override fun initialize(plugin: Plugin): Boolean {
        this.plugin = plugin
        try {
            // 检查 CraftEngine 是否已加载
            val craftEngine = Bukkit.getPluginManager().getPlugin("CraftEngine")
            if (craftEngine == null || !craftEngine.isEnabled) {
                return false
            }
            
            // 尝试加载 CraftEngine API 类
            val apiClass = Class.forName("net.momirealms.craftengine.bukkit.api.CraftEngineBlocks")
            
            // 创建方块提供者
            blockProvider = CraftEngineBlockProvider(apiClass, plugin)
            enabled = true
            
            plugin.logger.info("[CraftEngine] Successfully integrated (version: ${craftEngine.description.version})")
            return true
            
        } catch (e: ClassNotFoundException) {
            plugin.logger.warning("[CraftEngine] API class not found, version incompatible")
            if (plugin.config.getBoolean("debug", false)) {
                e.printStackTrace()
            }
            return false
        } catch (e: Exception) {
            plugin.logger.warning("[CraftEngine] Initialization failed: ${e.message}")
            if (plugin.config.getBoolean("debug", false)) {
                e.printStackTrace()
            }
            return false
        }
    }
    
    override fun isEnabled(): Boolean {
        return enabled
    }
    
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
 * CraftEngine 方块提供者
 */
class CraftEngineBlockProvider(private val apiClass: Class<*>, private val plugin: Plugin) : BlockProvider {
    
    override val providerName: String = "CraftEngine"
    private val debugEnabled: Boolean
        get() = plugin.config.getBoolean("debug", false)
    
    override fun isCustomBlock(block: Block): Boolean {
        return try {
            // CraftEngine 使用 getCustomBlockState 来检查，如果返回 null 则不是自定义方块
            val getStateMethod = apiClass.getMethod("getCustomBlockState", Block::class.java)
            val blockState = getStateMethod.invoke(null, block)
            val result = blockState != null
            
            if (debugEnabled && result) {
                plugin.logger.info("Debug: [CraftEngine] Block at ${block.location} is a custom block")
            }
            
            result
        } catch (e: Exception) {
            if (debugEnabled) {
                plugin.logger.warning("Debug: [CraftEngine] Error checking if block is custom: ${e.message}")
                e.printStackTrace()
            }
            false
        }
    }
    
    override fun getBlockId(block: Block): String? {
        return try {
            // 获取自定义方块状态 (ImmutableBlockState)
            val getStateMethod = apiClass.getMethod("getCustomBlockState", Block::class.java)
            val blockState = getStateMethod.invoke(null, block)
            
            if (blockState == null) {
                if (debugEnabled) {
                    plugin.logger.info("Debug: [CraftEngine] Block state is null for ${block.type} at ${block.location}")
                }
                return null
            }
            
            // 获取 owner (Holder<CustomBlock>)
            val ownerMethod = blockState.javaClass.getMethod("owner")
            val holder = ownerMethod.invoke(blockState)
            
            if (holder == null) {
                if (debugEnabled) {
                    plugin.logger.info("Debug: [CraftEngine] Holder is null")
                }
                return null
            }
            
            // 获取 CustomBlock
            val valueMethod = holder.javaClass.getMethod("value")
            val customBlock = valueMethod.invoke(holder)
            
            if (customBlock == null) {
                if (debugEnabled) {
                    plugin.logger.info("Debug: [CraftEngine] Custom block is null")
                }
                return null
            }
            
            // 获取 ID (Key 对象)
            val idMethod = customBlock.javaClass.getMethod("id")
            val keyObject = idMethod.invoke(customBlock)
            
            if (keyObject == null) {
                if (debugEnabled) {
                    plugin.logger.info("Debug: [CraftEngine] Key object is null")
                }
                return null
            }
            
            // Key 对象转字符串 (格式: namespace:id)
            val blockId = keyObject.toString()
            
            if (debugEnabled) {
                plugin.logger.info("Debug: [CraftEngine] Got block ID: $blockId")
            }
            
            blockId
        } catch (e: Exception) {
            if (debugEnabled) {
                plugin.logger.warning("Debug: [CraftEngine] Error getting block ID: ${e.message}")
                e.printStackTrace()
            }
            null
        }
    }
    
    override fun getBlockDisplayName(block: Block): String? {
        val id = getBlockId(block)
        if (id != null) {
            // 可以在这里实现从 CraftEngine 获取本地化名称的逻辑
            return id
        }
        return null
    }
}