package com.github.postyizhan.util.action

import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

/**
 * 动作执行器
 * 负责执行单个或多个动作，并提供执行结果反馈
 *
 * @author postyizhan
 */
class ActionExecutor(private val plugin: Plugin) {
    
    private val debugEnabled: Boolean
        get() = plugin is com.github.postyizhan.PostBits && plugin.isDebugEnabled()
    
    /**
     * 执行单个动作
     *
     * @param action 要执行的动作
     * @param player 触发动作的玩家
     * @return 是否执行成功
     */
    fun execute(action: Action, player: Player): Boolean {
        return try {
            if (debugEnabled) {
                plugin.logger.info("Debug: [ActionExecutor] Executing action for ${player.name}: ${action.getDescription()}")
            }
            
            val result = action.execute(player)
            
            if (debugEnabled) {
                plugin.logger.info("Debug: [ActionExecutor] Action result: $result")
            }
            
            result
        } catch (e: Exception) {
            plugin.logger.warning("[ActionExecutor] Failed to execute action: ${action.getDescription()} - ${e.message}")
            if (debugEnabled) {
                e.printStackTrace()
            }
            false
        }
    }
    
    /**
     * 执行动作列表
     *
     * @param actions 要执行的动作列表
     * @param player 触发动作的玩家
     * @return 成功执行的动作数量
     */
    fun executeAll(actions: List<Action>, player: Player): Int {
        var successCount = 0
        
        if (debugEnabled) {
            plugin.logger.info("Debug: [ActionExecutor] Executing ${actions.size} actions for ${player.name}")
        }
        
        for (action in actions) {
            if (execute(action, player)) {
                successCount++
            }
        }
        
        if (debugEnabled) {
            plugin.logger.info("Debug: [ActionExecutor] Successfully executed $successCount/${actions.size} actions")
        }
        
        return successCount
    }
    
    /**
     * 执行动作字符串列表
     *
     * @param actionStrings 动作字符串列表
     * @param player 触发动作的玩家
     * @return 成功执行的动作数量
     */
    fun executeFromStrings(actionStrings: List<String>, player: Player): Int {
        val actions = ActionParser.parseList(actionStrings, plugin)
        return executeAll(actions, player)
    }
}
