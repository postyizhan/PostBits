package com.github.postyizhan.integration

import org.bukkit.block.Block
import org.bukkit.plugin.Plugin

/**
 * 插件挂钩接口
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
     * 挂钩优先级（数值越小优先级越高）
     * 0-10: 最高优先级（核心插件）
     * 11-50: 高优先级
     * 51-100: 正常优先级
     * 101+: 低优先级
     */
    val priority: Int
        get() = 50
    
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
     * 获取方块提供者
     */
    fun getBlockProvider(): BlockProvider?
}

