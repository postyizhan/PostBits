package com.github.postyizhan.chair

import com.github.postyizhan.PostBits
import com.github.postyizhan.util.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.EquipmentSlot

/**
 * 椅子事件处理器 - 处理与椅子功能相关的事件
 * 参考 GSit 的事件处理实现
 * 
 * @author postyizhan
 */
class ChairEventHandler(private val plugin: PostBits, private val chairService: ChairService) : Listener {

    /**
     * 处理玩家右键方块事件 - 坐下
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        // 只处理右键方块事件
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        
        // 只处理主手操作（避免副手重复触发）
        if (event.hand != EquipmentSlot.HAND) return
        
        // 只处理点击方块顶部
        if (event.blockFace != BlockFace.UP) return
        
        val player = event.player
        val clickedBlock = event.clickedBlock ?: return
        
        // 检查权限
        if (!player.hasPermission("postbits.chair.sit")) {
            return
        }
        
        // 检查椅子模块是否启用
        if (!plugin.getConfigManager().getConfig().getBoolean("modules.chair.enabled", false)) {
            return
        }
        
        // 检查是否需要空手
        if (plugin.getConfigManager().getConfig().getBoolean("modules.chair.empty-hand-only", true)
            && event.item != null && event.item!!.type != Material.AIR) {
            return
        }
        
        // 检查玩家状态
        if (!player.isValid || player.isSneaking || player.gameMode == GameMode.SPECTATOR) {
            return
        }
        
        // 检查玩家是否已经在坐着
        if (chairService.isPlayerSitting(player)) {
            return
        }
        
        // 检查玩家是否被临时阻止
        if (chairService.isEntityBlocked(player)) {
            return
        }
        
        // 检查方块是否可以坐
        if (!chairService.isSittableBlock(clickedBlock)) {
            return
        }
        
        // 检查玩家是否在允许的世界
        val allowedWorlds = plugin.getConfigManager().getConfig().getStringList("modules.chair.allowed-worlds")
        if (allowedWorlds.isNotEmpty() && !allowedWorlds.contains(player.world.name)) {
            return
        }
        
        // 检查距离限制
        val maxDistance = plugin.getConfigManager().getConfig().getDouble("modules.chair.max-distance", 0.0)
        if (maxDistance > 0) {
            val blockCenter = clickedBlock.location.clone().add(0.5, 0.5, 0.5)
            if (player.location.distance(blockCenter) > maxDistance) {
                MessageUtil.sendMessage(player, "chair.too_far")
                return
            }
        }
        
        // 检查方块上方是否安全
        if (!plugin.getConfigManager().getConfig().getBoolean("modules.chair.allow-unsafe", false)) {
            val blockAbove = clickedBlock.getRelative(BlockFace.UP)
            // 检查上方是否为固体方块
            if (blockAbove.type.isSolid) {
                MessageUtil.sendMessage(player, "chair.unsafe_location")
                return
            }
        }
        
        // 获取点击位置（用于精确坐下位置）
        // 注意：interactionPoint 方法在 1.16+ 才可用
        // 这里默认使用中心位置，如需要精确位置需要额外实现
        val clickedX = 0.0
        val clickedZ = 0.0
        
        // 尝试让玩家坐下
        if (chairService.sitPlayer(player, clickedBlock, clickedX, clickedZ)) {
            event.isCancelled = true
            
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: Player ${player.name} sat on ${clickedBlock.type} at ${clickedBlock.location}")
            }
        }
    }
    
    /**
     * 处理玩家潜行事件 - 起身
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerToggleSneak(event: PlayerToggleSneakEvent) {
        // 只处理开始潜行
        if (!event.isSneaking) return
        
        val player = event.player
        
        // 检查玩家是否在坐着
        if (!chairService.isPlayerSitting(player)) return
        
        // 检查是否允许潜行起身
        if (!plugin.getConfigManager().getConfig().getBoolean("modules.chair.sneak-to-stand", true)) {
            return
        }
        
        // 让玩家起身
        chairService.standUpPlayer(player)
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: Player ${player.name} stood up by sneaking")
        }
    }
    
    /**
     * 处理玩家退出事件 - 自动起身
     */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        
        if (chairService.isPlayerSitting(player)) {
            chairService.standUpPlayer(player, returnToOriginal = true)
            
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: Player ${player.name} auto stood up on quit")
            }
        }
    }
    
    /**
     * 处理玩家死亡事件 - 自动起身
     */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        
        if (chairService.isPlayerSitting(player)) {
            chairService.standUpPlayer(player, returnToOriginal = false)
            
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: Player ${player.name} auto stood up on death")
            }
        }
    }
    
    /**
     * 处理玩家传送事件 - 远距离传送时起身
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        val player = event.player
        
        if (!chairService.isPlayerSitting(player)) return
        
        val seat = chairService.getPlayerSeat(player) ?: return
        
        // 检查是否跨世界传送
        if (event.to.world != seat.block.world) {
            chairService.standUpPlayer(player, returnToOriginal = false)
            
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: Player ${player.name} auto stood up on world change")
            }
            return
        }
        
        // 检查传送距离
        val distance = event.to.distance(seat.seatLocation)
        if (distance > 10.0) {
            chairService.standUpPlayer(player, returnToOriginal = false)
            
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: Player ${player.name} auto stood up on long distance teleport ($distance blocks)")
            }
        }
    }
    
    /**
     * 处理方块破坏事件 - 座位方块被破坏时起身
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val block = event.block
        
        // 获取该方块上的所有座位
        val seatsOnBlock = chairService.getSeatsOnBlock(block)
        
        if (seatsOnBlock.isNotEmpty()) {
            // 让所有坐在这个方块上的玩家起身
            seatsOnBlock.forEach { seat ->
                chairService.standUpPlayer(seat.player, returnToOriginal = false)
            }
            
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: Block ${block.type} broken, ${seatsOnBlock.size} players stood up")
            }
        }
    }
    
    /**
     * 处理玩家受伤事件 - 受伤时自动起身
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityDamage(event: EntityDamageEvent) {
        if (event.entity !is Player) return
        
        val player = event.entity as Player
        
        if (!chairService.isPlayerSitting(player)) return
        
        // 跳过摔落伤害（坐下时可能产生）
        if (event.cause == EntityDamageEvent.DamageCause.FALL) {
            event.isCancelled = true
            return
        }
        
        // 检查是否在受伤时起身
        if (plugin.getConfigManager().getConfig().getBoolean("modules.chair.stand-on-damage", true)) {
            chairService.standUpPlayer(player, returnToOriginal = false)
            
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: Player ${player.name} auto stood up on damage (${event.cause})")
            }
        }
    }
    
    /**
     * 处理玩家切换游戏模式事件 - 切换到观察者模式时起身
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onGameModeChange(event: PlayerGameModeChangeEvent) {
        if (event.newGameMode != GameMode.SPECTATOR) return
        
        val player = event.player
        
        if (chairService.isPlayerSitting(player)) {
            chairService.standUpPlayer(player, returnToOriginal = false)
            
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: Player ${player.name} auto stood up on gamemode change to SPECTATOR")
            }
        }
    }
    
    /**
     * 处理玩家移动事件 - 检测玩家是否离开座位
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerMove(event: PlayerMoveEvent) {
        // 只检查位置变化，忽略视角变化
        if (event.from.blockX == event.to.blockX && 
            event.from.blockY == event.to.blockY && 
            event.from.blockZ == event.to.blockZ) {
            return
        }
        
        val player = event.player
        val seat = chairService.getPlayerSeat(player) ?: return
        
        // 检查玩家是否还在座位实体上
        if (player.vehicle != seat.seatEntity) {
            // 延迟处理，避免与其他事件冲突
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                // 再次检查玩家是否还在坐着
                if (chairService.isPlayerSitting(player) && player.vehicle != seat.seatEntity) {
                    chairService.standUpPlayer(player, returnToOriginal = false)
                    
                    if (plugin.isDebugEnabled()) {
                        plugin.logger.info("Debug: Player ${player.name} auto stood up (left vehicle)")
                    }
                }
            }, 2L)
        }
    }
}
