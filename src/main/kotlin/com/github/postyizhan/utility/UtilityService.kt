package com.github.postyizhan.utility

import com.github.postyizhan.PostBits
import com.github.postyizhan.utility.commands.ItemCommands
import com.github.postyizhan.utility.commands.MovementCommands
import com.github.postyizhan.utility.commands.PlayerStateCommands
import com.github.postyizhan.utility.commands.VisibilityCommands

/**
 * 实用命令服务
 * 负责管理各种实用命令处理器
 * 
 * @author postyizhan
 */
class UtilityService(private val plugin: PostBits) {

    val playerStateCommands = PlayerStateCommands(plugin)
    val itemCommands = ItemCommands(plugin)
    val movementCommands = MovementCommands(plugin)
    val visibilityCommands = VisibilityCommands(plugin)

    fun initialize() {
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: UtilityService initialized")
        }
    }

    fun cleanup() {
        // 清理隐身状态
        visibilityCommands.cleanup()
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: UtilityService cleaned up")
        }
    }
}
