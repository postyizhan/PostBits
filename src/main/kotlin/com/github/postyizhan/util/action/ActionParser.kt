package com.github.postyizhan.util.action

import org.bukkit.plugin.Plugin

/**
 * 动作解析器
 * 负责解析配置中的动作字符串并创建对应的 Action 对象
 *
 * 支持的格式：
 * - [message] 消息内容
 * - [sound] 声音名称[:音量:音调]
 * - [console] 控制台命令
 * - [op] OP命令
 * - [player] 玩家命令
 *
 * @author postyizhan
 */
object ActionParser {
    
    private val ACTION_PATTERN = Regex("^\\[([^\\]]+)]\\s*(.*)$")
    
    /**
     * 解析单个动作字符串
     *
     * @param actionString 动作字符串
     * @param plugin 插件实例（用于调试日志）
     * @return 解析后的 Action 对象，如果解析失败则返回 null
     */
    fun parse(actionString: String, plugin: Plugin? = null): Action? {
        val trimmed = actionString.trim()
        if (trimmed.isEmpty()) {
            return null
        }
        
        val match = ACTION_PATTERN.find(trimmed)
        if (match == null) {
            plugin?.logger?.warning("[ActionParser] Invalid action format: $actionString")
            return null
        }
        
        val type = match.groupValues[1].trim().lowercase()
        val content = match.groupValues[2].trim()
        
        if (content.isEmpty()) {
            plugin?.logger?.warning("[ActionParser] Action content is empty: $actionString")
            return null
        }
        
        return try {
            when (type) {
                "message" -> MessageAction(content)
                
                "sound" -> parseSound(content)
                
                "console" -> ConsoleCommandAction(content)
                
                "op" -> OpCommandAction(content)
                
                "player" -> PlayerCommandAction(content)
                
                else -> {
                    plugin?.logger?.warning("[ActionParser] Unknown action type: $type")
                    null
                }
            }
        } catch (e: Exception) {
            plugin?.logger?.warning("[ActionParser] Failed to parse action: $actionString - ${e.message}")
            if (plugin is com.github.postyizhan.PostBits && plugin.isDebugEnabled()) {
                e.printStackTrace()
            }
            null
        }
    }
    
    /**
     * 解析声音动作
     * 格式: SOUND_NAME 或 SOUND_NAME:volume:pitch
     */
    private fun parseSound(content: String): SoundAction {
        val parts = content.split(":")
        val soundName = parts[0].trim()
        val volume = if (parts.size > 1) parts[1].toFloatOrNull() ?: 1.0f else 1.0f
        val pitch = if (parts.size > 2) parts[2].toFloatOrNull() ?: 1.0f else 1.0f
        
        return SoundAction(soundName, volume, pitch)
    }
    
    /**
     * 批量解析动作列表
     *
     * @param actionStrings 动作字符串列表
     * @param plugin 插件实例（用于调试日志）
     * @return 解析后的 Action 对象列表
     */
    fun parseList(actionStrings: List<String>, plugin: Plugin? = null): List<Action> {
        return actionStrings.mapNotNull { parse(it, plugin) }
    }
    
    /**
     * 验证动作字符串格式是否正确
     *
     * @param actionString 要验证的动作字符串
     * @return 是否为有效格式
     */
    fun isValid(actionString: String): Boolean {
        val trimmed = actionString.trim()
        if (trimmed.isEmpty()) {
            return false
        }
        
        val match = ACTION_PATTERN.find(trimmed)
        if (match == null) {
            return false
        }
        
        val type = match.groupValues[1].trim().lowercase()
        val content = match.groupValues[2].trim()
        
        if (content.isEmpty()) {
            return false
        }
        
        return when (type) {
            "message", "sound", "console", "op", "player" -> true
            else -> false
        }
    }
}
