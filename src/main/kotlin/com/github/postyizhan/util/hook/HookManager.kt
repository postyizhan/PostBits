package com.github.postyizhan.util.hook

import org.bukkit.plugin.Plugin

/**
 * 通用插件挂钩管理器
 * 负责管理所有第三方插件的挂钩
 * 
 * @author postyizhan
 */
class HookManager(private val plugin: Plugin) {
    
    // 存储所有已注册的挂钩
    private val hooks = mutableMapOf<String, PluginHook>()
    
    // 按类型分组的挂钩
    private val hooksByType = mutableMapOf<HookType, MutableList<PluginHook>>()
    
    /**
     * 注册一个插件挂钩
     */
    fun registerHook(hook: PluginHook) {
        hooks[hook.pluginName] = hook
        hooksByType.getOrPut(hook.hookType) { mutableListOf() }.add(hook)
        
        if (plugin is com.github.postyizhan.PostBits && plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: [HookManager] Registered hook: ${hook.pluginName} (type: ${hook.hookType})")
        }
    }
    
    /**
     * 初始化所有已注册的挂钩
     */
    fun initializeAll() {
        if (plugin is com.github.postyizhan.PostBits && plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: [HookManager] Initializing ${hooks.size} hooks...")
        }
        
        // 初始化所有挂钩
        hooks.values.forEach { hook ->
                try {
                    if (hook.initialize(plugin)) {
                        plugin.logger.info("[HookManager] ✓ ${hook.pluginName} hook initialized successfully")
                    } else {
                        if (plugin is com.github.postyizhan.PostBits && plugin.isDebugEnabled()) {
                            plugin.logger.info("Debug: [HookManager] ✗ ${hook.pluginName} hook initialization skipped")
                        }
                    }
                } catch (e: Exception) {
                    plugin.logger.warning("[HookManager] Failed to initialize ${hook.pluginName}: ${e.message}")
                    if (plugin is com.github.postyizhan.PostBits && plugin.isDebugEnabled()) {
                        e.printStackTrace()
                    }
                }
            }
    }
    
    /**
     * 卸载所有挂钩
     */
    fun unloadAll() {
        hooks.values.forEach { hook ->
            try {
                hook.unload()
                if (plugin is com.github.postyizhan.PostBits && plugin.isDebugEnabled()) {
                    plugin.logger.info("Debug: [HookManager] Unloaded hook: ${hook.pluginName}")
                }
            } catch (e: Exception) {
                plugin.logger.warning("[HookManager] Failed to unload ${hook.pluginName}: ${e.message}")
            }
        }
        hooks.clear()
        hooksByType.clear()
    }
    
    /**
     * 获取指定名称的挂钩
     */
    fun getHook(pluginName: String): PluginHook? {
        return hooks[pluginName]
    }
    
    /**
     * 获取指定类型的所有挂钩
     */
    fun getHooksByType(type: HookType): List<PluginHook> {
        return hooksByType[type] ?: emptyList()
    }
    
    /**
     * 获取挂钩提供的服务
     */
    fun <T> getService(pluginName: String, serviceClass: Class<T>): T? {
        return hooks[pluginName]?.getService(serviceClass)
    }
    
    /**
     * 检查指定插件的挂钩是否已启用
     */
    fun isEnabled(pluginName: String): Boolean {
        return hooks[pluginName]?.isEnabled() ?: false
    }
    
    /**
     * 获取所有已启用的挂钩
     */
    fun getEnabledHooks(): List<PluginHook> {
        return hooks.values.filter { it.isEnabled() }
    }
    
    // ===== 自定义方块相关方法 =====
    
    /**
     * 检查是否有方块提供者
     */
    fun hasBlockProviders(): Boolean {
        return getBlockProviders().isNotEmpty()
    }
    
    /**
     * 获取所有方块提供者
     */
    fun getBlockProviders(): List<com.github.postyizhan.util.BlockProvider> {
        return getHooksByType(HookType.CUSTOM_BLOCK)
            .filter { it.isEnabled() }
            .mapNotNull { hook ->
                try {
                    hook.getService(com.github.postyizhan.util.BlockProvider::class.java)
                } catch (e: Exception) {
                    null
                }
            }
    }
    
    /**
     * 检查方块是否为自定义方块
     */
    fun isCustomBlock(block: org.bukkit.block.Block): Boolean {
        return getBlockProviders().any { it.isCustomBlock(block) }
    }
    
    /**
     * 获取自定义方块的 ID
     */
    fun getCustomBlockId(block: org.bukkit.block.Block): String? {
        return getBlockProviders()
            .firstOrNull { it.isCustomBlock(block) }
            ?.getBlockId(block)
    }
    
    /**
     * 根据名称或别名获取提供者
     */
    fun getProviderByName(name: String): com.github.postyizhan.util.BlockProvider? {
        val normalizedName = name.lowercase()
        
        // 别名映射
        val aliases = mapOf(
            "ce" to "CraftEngine",
            "ia" to "ItemsAdder",
            "ox" to "Oraxen"
        )
        
        // 查找匹配的提供者
        val targetName = aliases[normalizedName] ?: name
        
        return getBlockProviders().firstOrNull { 
            it.providerName.equals(targetName, ignoreCase = true) 
        }
    }
}
