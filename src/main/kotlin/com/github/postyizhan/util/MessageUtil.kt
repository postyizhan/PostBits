package com.github.postyizhan.util

import com.github.postyizhan.PostBits
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.io.InputStreamReader

object MessageUtil {

    private lateinit var plugin: PostBits
    private lateinit var messages: FileConfiguration
    private var prefix: String = "&8[&3Post&bBits&8] "

    /**
     * 初始化消息工具
     */
    fun init(plugin: PostBits) {
        this.plugin = plugin
        loadMessages()
    }

    /**
     * 加载语言文件
     */
    private fun loadMessages() {
        val language = plugin.getConfigManager().getConfig().getString("language", "zh_CN")
        val langFile = File(plugin.dataFolder, "lang/$language.yml")
        
        // 如果语言文件不存在，创建默认语言文件
        if (!langFile.exists()) {
            plugin.saveResource("lang/$language.yml", false)
        }
        
        // 如果仍然不存在，使用内置的默认语言文件
        if (!langFile.exists()) {
            val inputStream = plugin.getResource("lang/$language.yml")
            if (inputStream != null) {
                messages = YamlConfiguration.loadConfiguration(InputStreamReader(inputStream, "UTF-8"))
            } else {
                // 如果连内置文件都没有，使用 zh_CN 作为后备
                val fallbackStream = plugin.getResource("lang/zh_CN.yml")
                if (fallbackStream != null) {
                    messages = YamlConfiguration.loadConfiguration(InputStreamReader(fallbackStream, "UTF-8"))
                } else {
                    // 创建空的配置作为最后的后备
                    messages = YamlConfiguration()
                    plugin.logger.warning("No language file found, using empty configuration")
                }
            }
        } else {
            messages = YamlConfiguration.loadConfiguration(langFile)
        }
        
        // 获取前缀
        prefix = messages.getString("prefix", "&8[&3Post&bBits&8] ")

        if (plugin.getConfigManager().getConfig().getBoolean("debug", false)) {
            plugin.logger.info("Debug: Language file loaded: $language")
        }
    }

    /**
     * 获取消息
     */
    fun getMessage(key: String): String {
        val message = messages.getString(key, "Missing message: $key")
        return message.replace("{prefix}", prefix)
    }

    /**
     * 发送消息给命令发送者
     */
    fun sendMessage(sender: CommandSender, key: String) {
        val message = getMessage(key)
        sender.sendMessage(color(message))
    }

    /**
     * 发送消息给玩家
     */
    fun sendMessage(player: Player, key: String) {
        val message = getMessage(key)
        player.sendMessage(color(message))
    }

    /**
     * 转换颜色代码
     */
    fun color(message: String): String {
        return ChatColor.translateAlternateColorCodes('&', message)
    }

    /**
     * 获取前缀
     */
    fun getPrefix(): String {
        return color(prefix)
    }

    /**
     * 重新加载消息
     */
    fun reload() {
        loadMessages()
        if (plugin.getConfigManager().getConfig().getBoolean("debug", false)) {
            plugin.logger.info("Debug: Messages reloaded")
        }
    }
}
