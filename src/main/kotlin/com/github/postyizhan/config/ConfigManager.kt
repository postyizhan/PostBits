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
        
        // 更新检查配置
        if (!config.contains("update-checker.enabled")) {
            config.set("update-checker.enabled", true)
        }
        if (!config.contains("update-checker.check-interval-days")) {
            config.set("update-checker.check-interval-days", 1)
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
