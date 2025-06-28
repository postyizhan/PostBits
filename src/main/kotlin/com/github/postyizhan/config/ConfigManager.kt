package com.github.postyizhan.config

import com.github.postyizhan.PostBits
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class ConfigManager(private val plugin: PostBits) {

    private lateinit var config: FileConfiguration
    private lateinit var configFile: File

    /**
     * 加载所有配置文件
     */
    fun loadAll() {
        loadConfig()
        // 在配置加载后检查调试模式
        if (config.getBoolean("debug", false)) {
            plugin.logger.info("Debug: All configuration files loaded")
        }
    }

    /**
     * 加载主配置文件
     */
    private fun loadConfig() {
        configFile = File(plugin.dataFolder, "config.yml")
        
        // 如果配置文件不存在，创建默认配置
        if (!configFile.exists()) {
            plugin.saveDefaultConfig()
        }
        
        config = YamlConfiguration.loadConfiguration(configFile)
        
        // 添加默认配置项
        populateDefaults()
        
        // 保存配置文件
        saveConfig()
    }

    /**
     * 添加默认配置项
     */
    private fun populateDefaults() {
        // 模块配置
        if (!config.contains("modules.update-checker.enabled")) {
            config.set("modules.update-checker.enabled", false)
        }
        if (!config.contains("modules.update-checker.check-interval-days")) {
            config.set("modules.update-checker.check-interval-days", 1)
        }

        // 椅子模块配置
        if (!config.contains("modules.chair.enabled")) {
            config.set("modules.chair.enabled", false)
        }
        if (!config.contains("modules.chair.sittable-blocks")) {
            config.set("modules.chair.sittable-blocks", listOf(
                "oak_stairs", "birch_stairs", "spruce_stairs", "jungle_stairs",
                "acacia_stairs", "dark_oak_stairs", "stone_stairs", "brick_stairs",
                "stone_brick_stairs", "nether_brick_stairs", "sandstone_stairs", "quartz_stairs",
                "oak_slab", "birch_slab", "spruce_slab", "jungle_slab",
                "acacia_slab", "dark_oak_slab", "stone_slab", "stone_brick_slab"
            ))
        }
        if (!config.contains("modules.chair.empty-hand-only")) {
            config.set("modules.chair.empty-hand-only", true)
        }
        if (!config.contains("modules.chair.max-distance")) {
            config.set("modules.chair.max-distance", 3.0)
        }
        if (!config.contains("modules.chair.allow-unsafe")) {
            config.set("modules.chair.allow-unsafe", false)
        }
        if (!config.contains("modules.chair.allow-multiple-players")) {
            config.set("modules.chair.allow-multiple-players", false)
        }
        if (!config.contains("modules.chair.sneak-to-stand")) {
            config.set("modules.chair.sneak-to-stand", true)
        }
        if (!config.contains("modules.chair.stand-on-damage")) {
            config.set("modules.chair.stand-on-damage", true)
        }
        if (!config.contains("modules.chair.return-to-original")) {
            config.set("modules.chair.return-to-original", false)
        }
        
        // 语言配置
        if (!config.contains("language")) {
            config.set("language", "zh_CN")
        }
        
        // 调试模式
        if (!config.contains("debug")) {
            config.set("debug", false)
        }
    }

    /**
     * 保存主配置文件
     */
    private fun saveConfig() {
        try {
            config.save(configFile)
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save config.yml: ${e.message}")
        }
    }

    /**
     * 获取主配置文件
     */
    fun getConfig(): FileConfiguration {
        return config
    }

    /**
     * 重新加载主配置文件
     */
    fun reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile)
        populateDefaults()
        saveConfig()
    }
}
