package com.github.postyizhan.chair

import com.github.postyizhan.PostBits
import com.github.postyizhan.util.MessageUtil
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.EquipmentSlot

/**
 * 椅子事件处理器 - 处理与椅子功能相关的事件
 * 
 * @author postyizhan
 */
class ChairEventHandler(private val plugin: PostBits, private val chairService: ChairService) : Listener {

    /**
     * 处理玩家右键方块事件
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        // 只处理右键方块事件
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        
        // 只处理主手操作
        if (event.hand != EquipmentSlot.HAND) return
        
        // 只处理点击方块顶部
        if (event.blockFace != BlockFace.UP) return
        
        val player = event.player
        val block = event.clickedBlock ?: return
        
        // 检查椅子模块是否启用
        if (!plugin.getConfigManager().getConfig().getBoolean("modules.chair.enabled", false)) {
            return
        }
        
        // 检查权限
        if (!player.hasPermission("postbits.chair.sit")) {
            return
        }
        
        // 检查是否需要空手
        if (plugin.getConfigManager().getConfig().getBoolean("modules.chair.empty-hand-only", true)
            && player.inventory.itemInMainHand.type != org.bukkit.Material.AIR) {
            return
        }
        
        // 检查玩家是否已经在坐着
        if (chairService.isPlayerSitting(player)) {
            return
        }
        
        // 检查玩家状态
        if (!player.isValid || player.isSneaking || !player.isOnGround || 
            player.vehicle != null || player.isSleeping) {
            return
        }
        
        // 检查方块是否可以坐
        if (!chairService.isSittableBlock(block)) {
            return
        }
        
        // 检查距离限制
        val maxDistance = plugin.getConfigManager().getConfig().getDouble("modules.chair.max-distance", 3.0)
        if (maxDistance > 0 && player.location.distance(block.location.add(0.5, 0.5, 0.5)) > maxDistance) {
            MessageUtil.sendMessage(player, "chair.too_far")
            return
        }
        
        // 检查方块上方是否安全
        if (!plugin.getConfigManager().getConfig().getBoolean("modules.chair.allow-unsafe", false)) {
            val blockAbove = block.getRelative(BlockFace.UP)
            if (blockAbove.type != org.bukkit.Material.AIR) {
                MessageUtil.sendMessage(player, "chair.unsafe_location")
                return
            }
        }
        
        // 尝试让玩家坐下
        if (chairService.sitPlayer(player, block)) {
            event.isCancelled = true
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: Player ${player.name} sat on ${block.type} at ${block.location}")
            }
        }
    }
    
    /**
     * 处理玩家移动事件（检测是否离开座位）
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val seat = chairService.getPlayerSeat(player) ?: return

        // 检查玩家是否还在盔甲架上
        if (player.vehicle != seat.armorStand) {
            // 延迟处理，避免与其他插件冲突
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                if (!chairService.isPlayerSitting(player)) return@Runnable
                chairService.standUpPlayer(player)
            }, 1L)
        }
    }
    
    /**
     * 处理玩家潜行事件（起身）
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerToggleSneak(event: PlayerToggleSneakEvent) {
        if (!event.isSneaking) return
        
        val player = event.player
        if (!chairService.isPlayerSitting(player)) return
        
        // 检查是否允许潜行起身
        if (plugin.getConfigManager().getConfig().getBoolean("modules.chair.sneak-to-stand", true)) {
            chairService.standUpPlayer(player)
        }
    }
    
    /**
     * 处理玩家退出事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        if (chairService.isPlayerSitting(player)) {
            chairService.standUpPlayer(player)
        }
    }
    
    /**
     * 处理玩家传送事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        val player = event.player
        if (chairService.isPlayerSitting(player)) {
            // 如果传送距离较远，让玩家起身
            val seat = chairService.getPlayerSeat(player)
            if (seat != null && event.to.world != seat.seatLocation.world ||
                event.to.distance(seat!!.seatLocation) > 10) {
                chairService.standUpPlayer(player)
            }
        }
    }
    
    /**
     * 处理方块破坏事件
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val block = event.block
        val seats = chairService.getSeatsOnBlock(block)
        
        if (seats.isNotEmpty()) {
            // 让所有坐在这个方块上的玩家起身
            seats.forEach { seat ->
                chairService.standUpPlayer(seat.player)
            }
            
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: Block broken, ${seats.size} players stood up")
            }
        }
    }
    
    /**
     * 处理玩家受伤事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerDamage(event: EntityDamageEvent) {
        if (event.entity !is Player) return
        
        val player = event.entity as Player
        if (!chairService.isPlayerSitting(player)) return
        
        // 检查是否在受伤时起身
        if (plugin.getConfigManager().getConfig().getBoolean("modules.chair.stand-on-damage", true)) {
            chairService.standUpPlayer(player)
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: Player ${player.name} stood up due to damage")
            }
        }
    }
}
