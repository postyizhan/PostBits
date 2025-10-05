package com.github.postyizhan.keybind

import com.github.postyizhan.PostBits
import com.github.postyizhan.util.action.Action
import com.github.postyizhan.util.action.ActionExecutor
import com.github.postyizhan.util.action.ActionParser
import org.bukkit.entity.Player

/**
 * 按键绑定服务
 * 管理玩家按键和动作的映射关系
 *
 * @author postyizhan
 */
class KeyBindService(private val plugin: PostBits) {
    
    private val actionExecutor = ActionExecutor(plugin)
    private val keyBindings = mutableMapOf<KeyType, List<Action>>()
    
    init {
        loadKeyBindings()
    }
    
    /**
     * 从配置加载按键绑定
     */
    private fun loadKeyBindings() {
        keyBindings.clear()
        
        val config = plugin.getConfigManager().getConfig()
        val keybindSection = config.getConfigurationSection("modules.keybind.bindings")
        
        if (keybindSection == null) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: [KeyBind] No keybind configuration found")
            }
            return
        }
        
        // 遍历所有按键配置
        for (keyName in keybindSection.getKeys(false)) {
            try {
                if (plugin.isDebugEnabled()) {
                    plugin.logger.info("Debug: [KeyBind] Processing key config: $keyName")
                }
                
                val keyType = KeyType.fromConfigName(keyName)
                if (keyType == null) {
                    plugin.logger.warning("[KeyBind] Unknown key type: $keyName")
                    continue
                }
                
                if (plugin.isDebugEnabled()) {
                    plugin.logger.info("Debug: [KeyBind] Matched to KeyType: ${keyType.name}")
                }
                
                val actionStrings = keybindSection.getStringList("$keyName.actions")
                if (plugin.isDebugEnabled()) {
                    plugin.logger.info("Debug: [KeyBind] Found ${actionStrings.size} action strings for $keyName: $actionStrings")
                }
                
                if (actionStrings.isEmpty()) {
                    if (plugin.isDebugEnabled()) {
                        plugin.logger.info("Debug: [KeyBind] No actions configured for key: $keyName, skipping")
                    }
                    continue
                }
                
                val actions = ActionParser.parseList(actionStrings, plugin)
                if (plugin.isDebugEnabled()) {
                    plugin.logger.info("Debug: [KeyBind] Parsed ${actions.size} valid actions for $keyName")
                }
                
                if (actions.isNotEmpty()) {
                    keyBindings[keyType] = actions
                    plugin.logger.info("[KeyBind] Loaded ${actions.size} action(s) for key: ${keyType.displayName} ($keyName)")
                } else {
                    if (plugin.isDebugEnabled()) {
                        plugin.logger.info("Debug: [KeyBind] No valid actions after parsing for $keyName")
                    }
                }
            } catch (e: Exception) {
                plugin.logger.warning("[KeyBind] Failed to load keybind for $keyName: ${e.message}")
                if (plugin.isDebugEnabled()) {
                    e.printStackTrace()
                }
            }
        }
        
        plugin.logger.info("[KeyBind] Loaded ${keyBindings.size} key bindings")
    }
    
    /**
     * 处理玩家按键事件
     *
     * @param player 按下按键的玩家
     * @param keyType 按键类型
     */
    fun handleKeyPress(player: Player, keyType: KeyType) {
        val actions = keyBindings[keyType]
        if (actions == null || actions.isEmpty()) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: [KeyBind] No actions bound to key: ${keyType.name} (${keyType.configName})")
                plugin.logger.info("Debug: [KeyBind] Currently loaded bindings: ${keyBindings.keys.map { it.name }}")
            }
            return
        }
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: [KeyBind] Player ${player.name} pressed key: ${keyType.displayName}, executing ${actions.size} action(s)")
        }
        
        actionExecutor.executeAll(actions, player)
    }
    
    /**
     * 获取指定按键绑定的动作数量
     */
    fun getActionCount(keyType: KeyType): Int {
        return keyBindings[keyType]?.size ?: 0
    }
    
    /**
     * 重新加载配置
     */
    fun reload() {
        loadKeyBindings()
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        keyBindings.clear()
    }
}

/**
 * 按键类型枚举
 * 定义所有支持的按键组合
 */
enum class KeyType(val configName: String, val displayName: String) {
    /**
     * 按下 Shift（潜行）
     */
    SNEAK("sneak", "Shift"),
    
    /**
     * 按下 F（切换副手）
     */
    SWAP_HAND("swap_hand", "F"),
    
    /**
     * 按下 Shift + F（切换副手）
     */
    SNEAK_SWAP("sneak_swap", "Shift+F"),
    
    /**
     * 按下 Q（丢弃物品）
     */
    DROP("drop", "Q");
    
    companion object {
        /**
         * 根据配置名称获取按键类型
         */
        fun fromConfigName(name: String): KeyType? {
            return values().firstOrNull { it.configName.equals(name, ignoreCase = true) }
        }
    }
}
