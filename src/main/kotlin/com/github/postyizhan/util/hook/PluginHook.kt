package com.github.postyizhan.util.hook

import org.bukkit.plugin.Plugin

/**
 * 通用插件挂钩接口
 * 所有第三方插件集成都应该实现此接口
 * 
 * @author postyizhan
 */
interface PluginHook {
    
    /**
     * 挂钩的插件名称
     */
    val pluginName: String
    
    /**
     * 挂钩类型
     * 用于标识插件提供的功能类型
     */
    val hookType: HookType
        get() = HookType.GENERIC
    
    /**
     * 初始化挂钩
     * 
     * @param plugin PostBits 插件实例
     * @return 是否初始化成功
     */
    fun initialize(plugin: Plugin): Boolean
    
    /**
     * 检查挂钩是否已启用
     */
    fun isEnabled(): Boolean
    
    /**
     * 卸载挂钩
     */
    fun unload()
    
    /**
     * 获取挂钩提供的服务（可选）
     * 不同类型的挂钩可以返回不同的服务实例
     */
    fun <T> getService(serviceClass: Class<T>): T? {
        return null
    }
}

/**
 * 挂钩类型枚举
 */
enum class HookType {
    /**
     * 通用插件（无特定功能分类）
     */
    GENERIC,
    
    /**
     * 协议处理（如 ProtocolLib）
     */
    PROTOCOL,
    
    /**
     * 自定义方块提供者（如 CraftEngine, ItemsAdder, Oraxen）
     */
    CUSTOM_BLOCK,
    
    /**
     * 权限管理
     */
    PERMISSION,
    
    /**
     * 经济系统
     */
    ECONOMY,
    
    /**
     * 世界管理
     */
    WORLD
}
