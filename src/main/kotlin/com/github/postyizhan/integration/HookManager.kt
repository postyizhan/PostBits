package com.github.postyizhan.integration

import org.bukkit.block.Block
import org.bukkit.plugin.Plugin
import java.util.concurrent.ConcurrentHashMap

/**
 * 插件挂钩管理器
 * 统一管理所有第三方插件的集成
 * 
 * @author postyizhan
 */
class HookManager(private val plugin: Plugin) {
    
    private val hooks = ConcurrentHashMap<String, PluginHook>()
    private val blockProviders = mutableListOf<BlockProvider>()
    private var initialized = false
    
    companion object {
        // 内置的提供者别名映射
        private val PROVIDER_ALIASES = mapOf(
            "ce" to "CraftEngine",
            "craftengine" to "CraftEngine",
            "ia" to "ItemsAdder",
            "itemsadder" to "ItemsAdder",
            "ox" to "Oraxen",
            "oraxen" to "Oraxen"
        )
    }
    
    /**
     * 注册插件挂钩
     */
    fun registerHook(hook: PluginHook) {
        if (initialized) {
            plugin.logger.warning("[HookManager] Cannot register hook '${hook.pluginName}' after initialization")
            return
        }
        
        hooks[hook.pluginName] = hook
        plugin.logger.info("[HookManager] Registered hook: ${hook.pluginName} (priority: ${hook.priority})")
    }
    
    /**
     * 初始化所有挂钩
     */
    fun initializeAll() {
        if (initialized) {
            plugin.logger.warning("[HookManager] Already initialized")
            return
        }
        
        plugin.logger.info("[HookManager] Initializing plugin hooks...")
        
        // 按优先级排序
        val sortedHooks = hooks.values.sortedBy { it.priority }
        
        var successCount = 0
        for (hook in sortedHooks) {
            try {
                if (hook.initialize(plugin)) {
                    plugin.logger.info("[HookManager] ✓ ${hook.pluginName} hook initialized successfully")
                    
                    // 注册方块提供者
                    hook.getBlockProvider()?.let { provider ->
                        blockProviders.add(provider)
                        plugin.logger.info("[HookManager]   - Registered block provider: ${provider.providerName}")
                    }
                    
                    successCount++
                } else {
                    plugin.logger.info("[HookManager] ✗ ${hook.pluginName} hook not available")
                }
            } catch (e: Exception) {
                plugin.logger.warning("[HookManager] ✗ Failed to initialize ${hook.pluginName} hook: ${e.message}")
                if (plugin.config.getBoolean("debug", false)) {
                    e.printStackTrace()
                }
            }
        }
        
        initialized = true
        plugin.logger.info("[HookManager] Initialized $successCount/${hooks.size} plugin hooks")
        plugin.logger.info("[HookManager] Registered ${blockProviders.size} block providers")
    }
    
    /**
     * 卸载所有挂钩
     */
    fun unloadAll() {
        plugin.logger.info("[HookManager] Unloading all plugin hooks...")
        
        for (hook in hooks.values) {
            try {
                if (hook.isEnabled()) {
                    hook.unload()
                    plugin.logger.info("[HookManager] Unloaded: ${hook.pluginName}")
                }
            } catch (e: Exception) {
                plugin.logger.warning("[HookManager] Error unloading ${hook.pluginName}: ${e.message}")
            }
        }
        
        blockProviders.clear()
        initialized = false
    }
    
    /**
     * 检查方块是否为任何已注册提供者的自定义方块
     */
    fun isCustomBlock(block: Block): Boolean {
        return blockProviders.any { it.isCustomBlock(block) }
    }
    
    /**
     * 获取自定义方块的 ID（从第一个识别的提供者）
     */
    fun getCustomBlockId(block: Block): String? {
        for (provider in blockProviders) {
            if (provider.isCustomBlock(block)) {
                return provider.getBlockId(block)
            }
        }
        return null
    }
    
    /**
     * 获取方块的显示名称
     */
    fun getBlockDisplayName(block: Block): String {
        for (provider in blockProviders) {
            if (provider.isCustomBlock(block)) {
                provider.getBlockDisplayName(block)?.let { return it }
            }
        }
        return block.type.name
    }
    
    /**
     * 检查方块是否匹配任何提供者的 ID 模式
     */
    fun matchesCustomBlockId(block: Block, patterns: List<String>): Boolean {
        if (patterns.isEmpty()) {
            return false
        }
        
        for (provider in blockProviders) {
            if (provider.matchesBlockId(block, patterns)) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * 获取所有已启用的挂钩
     */
    fun getEnabledHooks(): List<PluginHook> {
        return hooks.values.filter { it.isEnabled() }
    }
    
    /**
     * 获取特定的挂钩
     */
    fun getHook(pluginName: String): PluginHook? {
        return hooks[pluginName]
    }
    
    /**
     * 获取所有方块提供者
     */
    fun getBlockProviders(): List<BlockProvider> {
        return blockProviders.toList()
    }
    
    /**
     * 检查是否有任何方块提供者
     */
    fun hasBlockProviders(): Boolean {
        return blockProviders.isNotEmpty()
    }
    
    /**
     * 根据提供者名称或别名获取方块提供者
     * 
     * @param providerName 提供者名称或别名（如 "CraftEngine", "ce", "ItemsAdder", "ia"）
     * @return 匹配的 BlockProvider，如果未找到则返回 null
     */
    fun getProviderByName(providerName: String): BlockProvider? {
        // 先尝试通过别名解析为实际插件名
        val actualName = PROVIDER_ALIASES[providerName.lowercase()] ?: providerName
        
        // 查找匹配的提供者
        return blockProviders.find { 
            it.providerName.equals(actualName, ignoreCase = true) 
        }
    }
    
    /**
     * 检查指定提供者的方块是否匹配 ID 模式
     * 
     * @param providerName 提供者名称或别名
     * @param block 要检查的方块
     * @param patterns ID 模式列表
     * @return 是否匹配
     */
    fun matchesCustomBlockIdWithProvider(providerName: String, block: Block, patterns: List<String>): Boolean {
        val provider = getProviderByName(providerName) ?: return false
        return provider.matchesBlockId(block, patterns)
    }
}

