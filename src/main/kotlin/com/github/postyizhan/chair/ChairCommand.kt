package com.github.postyizhan.chair

import com.github.postyizhan.PostBits
import com.github.postyizhan.util.MessageUtil
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * 椅子命令处理器 - 处理椅子相关的命令
 * 
 * @author postyizhan
 */
class ChairCommand(private val plugin: PostBits, private val chairService: ChairService) {

    /**
     * 处理椅子相关命令
     */
    fun handleChairCommand(sender: CommandSender, args: Array<out String>): Boolean {
        if (sender !is Player) {
            MessageUtil.sendMessage(sender, "messages.player_only")
            return true
        }

        // 检查椅子模块是否启用
        if (!plugin.getConfigManager().getConfig().getBoolean("modules.chair.enabled", false)) {
            MessageUtil.sendMessage(sender, "messages.module_disabled")
            return true
        }

        if (args.isEmpty()) {
            // 切换坐下/起身状态
            return toggleSitState(sender)
        }

        when (args[0].lowercase()) {
            "sit" -> {
                return handleSitCommand(sender)
            }
            "stand", "up" -> {
                return handleStandCommand(sender)
            }
            "info" -> {
                return handleInfoCommand(sender)
            }
            else -> {
                MessageUtil.sendMessage(sender, "chair.unknown_subcommand")
                return true
            }
        }
    }

    /**
     * 切换坐下/起身状态
     */
    private fun toggleSitState(player: Player): Boolean {
        if (chairService.isPlayerSitting(player)) {
            return handleStandCommand(player)
        } else {
            return handleSitCommand(player)
        }
    }

    /**
     * 处理坐下命令
     */
    private fun handleSitCommand(player: Player): Boolean {
        if (!player.hasPermission("postbits.chair.sit")) {
            MessageUtil.sendMessage(player, "messages.no_permission")
            return true
        }

        if (chairService.isPlayerSitting(player)) {
            MessageUtil.sendMessage(player, "chair.already_sitting")
            return true
        }

        // 检查玩家状态
        if (!player.isValid || player.isSneaking || !player.isOnGround || 
            player.vehicle != null || player.isSleeping) {
            MessageUtil.sendMessage(player, "chair.invalid_state")
            return true
        }

        // 获取玩家脚下的方块
        val block = player.location.block.getRelative(org.bukkit.block.BlockFace.DOWN)
        
        // 检查方块是否可以坐
        if (!chairService.isSittableBlock(block)) {
            MessageUtil.sendMessage(player, "chair.not_sittable")
            return true
        }

        // 检查方块上方是否安全
        if (!plugin.getConfigManager().getConfig().getBoolean("modules.chair.allow-unsafe", false)) {
            val blockAbove = block.getRelative(org.bukkit.block.BlockFace.UP)
            if (blockAbove.type != org.bukkit.Material.AIR) {
                MessageUtil.sendMessage(player, "chair.unsafe_location")
                return true
            }
        }

        // 尝试让玩家坐下
        if (chairService.sitPlayer(player, block)) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: Player ${player.name} sat via command")
            }
        } else {
            MessageUtil.sendMessage(player, "chair.sit_failed")
        }

        return true
    }

    /**
     * 处理起身命令
     */
    private fun handleStandCommand(player: Player): Boolean {
        if (!player.hasPermission("postbits.chair.sit")) {
            MessageUtil.sendMessage(player, "messages.no_permission")
            return true
        }

        if (!chairService.isPlayerSitting(player)) {
            MessageUtil.sendMessage(player, "chair.not_sitting")
            return true
        }

        if (chairService.standUpPlayer(player)) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: Player ${player.name} stood up via command")
            }
        } else {
            MessageUtil.sendMessage(player, "chair.stand_failed")
        }

        return true
    }

    /**
     * 处理信息命令
     */
    private fun handleInfoCommand(player: Player): Boolean {
        if (!player.hasPermission("postbits.chair.info")) {
            MessageUtil.sendMessage(player, "messages.no_permission")
            return true
        }

        val seat = chairService.getPlayerSeat(player)
        if (seat == null) {
            MessageUtil.sendMessage(player, "chair.not_sitting")
            return true
        }

        // 发送座位信息
        val duration = seat.getSitDuration() / 1000 // 转换为秒
        val blockType = seat.block.type.name
        val location = seat.block.location
        
        MessageUtil.sendMessage(player, "chair.info_header")
        player.sendMessage("§7方块类型: §e${blockType}")
        player.sendMessage("§7位置: §e${location.blockX}, ${location.blockY}, ${location.blockZ}")
        player.sendMessage("§7坐下时长: §e${duration}秒")

        return true
    }
}
