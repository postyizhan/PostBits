package com.github.postyizhan.module

import com.github.postyizhan.PostBits
import com.github.postyizhan.chair.ChairEventHandler
import com.github.postyizhan.chair.ChairService
import com.github.postyizhan.elevator.ElevatorEventHandler
import com.github.postyizhan.elevator.ElevatorService
import com.github.postyizhan.utility.UtilityService
import com.github.postyizhan.invedit.InvEditEventHandler
import com.github.postyizhan.invedit.InvEditService
import com.github.postyizhan.portabletools.PortableToolsService

/**
 * 模块管理器 - 统一管理所有功能模块的初始化和清理
 *
 * @author postyizhan
 */
class ModuleManager(private val plugin: PostBits) {

    private val modules = mutableMapOf<String, Module>()

    /**
     * 初始化所有启用的模块
     */
    fun initializeModules() {
        val config = plugin.getConfigManager().getConfig()
        
        // 椅子模块
        registerModule("chair", ChairModule(plugin))
        
        // 背包编辑模块
        registerModule("invedit", InvEditModule(plugin))
        
        // 电梯模块
        registerModule("elevator", ElevatorModule(plugin))
        
        // 实用命令模块
        registerModule("utility", UtilityModule(plugin))
        
        // 随身工具模块
        registerModule("portabletools", PortableToolsModule(plugin))
        
        // 启用所有模块
        modules.forEach { (name, module) ->
            if (config.getBoolean("modules.$name.enabled", false)) {
                module.enable()
                if (plugin.isDebugEnabled()) {
                    plugin.logger.info("Debug: $name module enabled")
                }
            }
        }
    }

    /**
     * 重新加载所有模块
     */
    fun reloadModules() {
        val config = plugin.getConfigManager().getConfig()
        
        modules.forEach { (name, module) ->
            val shouldBeEnabled = config.getBoolean("modules.$name.enabled", false)
            
            if (shouldBeEnabled && !module.isEnabled()) {
                module.enable()
            } else if (!shouldBeEnabled && module.isEnabled()) {
                module.disable()
            }
        }
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: All modules reloaded")
        }
    }

    /**
     * 清理所有模块
     */
    fun cleanupModules() {
        modules.values.forEach { it.disable() }
    }

    /**
     * 注册模块
     */
    private fun registerModule(name: String, module: Module) {
        modules[name] = module
    }

    /**
     * 获取模块
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Module> getModule(name: String): T? {
        return modules[name] as? T
    }
}

/**
 * 模块接口
 */
interface Module {
    fun enable()
    fun disable()
    fun isEnabled(): Boolean
}

/**
 * 抽象模块基类
 * 封装了模块的通用生命周期管理逻辑
 *
 * @param T 服务类型
 */
abstract class BaseModule<T : Any>(protected val plugin: PostBits) : Module {
    private var service: T? = null

    override fun enable() {
        if (service == null) {
            service = createService()
            onServiceCreated(service!!)
        }
    }

    override fun disable() {
        service?.let { onServiceDestroy(it) }
        service = null
    }

    override fun isEnabled(): Boolean = service != null

    /**
     * 获取服务实例
     */
    fun getService(): T? = service

    /**
     * 创建服务实例
     */
    protected abstract fun createService(): T

    /**
     * 服务创建后的回调
     * 用于注册事件监听器等初始化操作
     */
    protected open fun onServiceCreated(service: T) {}

    /**
     * 服务销毁前的回调
     * 用于清理资源、注销监听器等
     */
    protected open fun onServiceDestroy(service: T) {
        // 使用反射调用 cleanup 方法（如果存在）
        try {
            val cleanupMethod = service::class.java.getMethod("cleanup")
            cleanupMethod.invoke(service)
        } catch (e: NoSuchMethodException) {
            // 服务没有 cleanup 方法，忽略
        } catch (e: Exception) {
            plugin.logger.warning("Failed to cleanup service: ${e.message}")
        }
    }
}

/**
 * 椅子模块
 */
class ChairModule(plugin: PostBits) : BaseModule<ChairService>(plugin) {
    override fun createService() = ChairService(plugin)

    override fun onServiceCreated(service: ChairService) {
        val eventHandler = ChairEventHandler(plugin, service)
        plugin.server.pluginManager.registerEvents(eventHandler, plugin)
    }
}

/**
 * 背包编辑模块
 */
class InvEditModule(plugin: PostBits) : BaseModule<InvEditService>(plugin) {
    override fun createService() = InvEditService(plugin)

    override fun onServiceCreated(service: InvEditService) {
        val eventHandler = InvEditEventHandler(plugin, service)
        plugin.server.pluginManager.registerEvents(eventHandler, plugin)
    }
}

/**
 * 电梯模块
 */
class ElevatorModule(plugin: PostBits) : BaseModule<ElevatorService>(plugin) {
    override fun createService() = ElevatorService(plugin)

    override fun onServiceCreated(service: ElevatorService) {
        val eventHandler = ElevatorEventHandler(plugin, service)
        plugin.server.pluginManager.registerEvents(eventHandler, plugin)
    }
}

/**
 * 实用命令模块
 */
class UtilityModule(plugin: PostBits) : BaseModule<UtilityService>(plugin) {
    override fun createService() = UtilityService(plugin)

    override fun onServiceCreated(service: UtilityService) {
        service.initialize()
        
        // 注册事件处理器（用于 vanish 功能）
        val eventHandler = com.github.postyizhan.utility.UtilityEventHandler(plugin, service)
        plugin.server.pluginManager.registerEvents(eventHandler, plugin)
    }
}

/**
 * 随身工具模块
 */
class PortableToolsModule(plugin: PostBits) : BaseModule<PortableToolsService>(plugin) {
    override fun createService() = PortableToolsService(plugin)

    override fun onServiceCreated(service: PortableToolsService) {
        service.initialize()
    }
}
