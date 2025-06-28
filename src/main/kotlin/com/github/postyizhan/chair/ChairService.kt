package com.github.postyizhan.chair

import com.github.postyizhan.PostBits
import com.github.postyizhan.util.MessageUtil
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 椅子服务类 - 管理玩家坐下功能
 * 
 * @author postyizhan
 */
class ChairService(private val plugin: PostBits) {

    // 存储所有座位信息
    private val seats = ConcurrentHashMap<UUID, ChairSeat>()
    
    // 存储方块对应的座位
    private val blockSeats = ConcurrentHashMap<Block, MutableSet<ChairSeat>>()

    /**
     * 检查玩家是否正在坐着
     */
    fun isPlayerSitting(player: Player): Boolean {
        return seats.containsKey(player.uniqueId)
    }

    /**
     * 获取玩家的座位信息
     */
    fun getPlayerSeat(player: Player): ChairSeat? {
        return seats[player.uniqueId]
    }

    /**
     * 检查方块是否有人坐着
     */
    fun isBlockOccupied(block: Block): Boolean {
        return blockSeats[block]?.isNotEmpty() == true
    }

    /**
     * 获取方块上的所有座位
     */
    fun getSeatsOnBlock(block: Block): Set<ChairSeat> {
        return blockSeats[block]?.toSet() ?: emptySet()
    }

    /**
     * 让玩家坐在指定方块上
     */
    fun sitPlayer(player: Player, block: Block): Boolean {
        if (isPlayerSitting(player)) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: Player ${player.name} is already sitting")
            }
            return false
        }

        // 检查方块是否被占用（如果不允许多人坐同一方块）
        if (!plugin.getConfigManager().getConfig().getBoolean("modules.chair.allow-multiple-players", true) 
            && isBlockOccupied(block)) {
            MessageUtil.sendMessage(player, "chair.already_occupied")
            return false
        }

        // 计算座位位置
        val seatLocation = calculateSeatLocation(block, player)
        
        // 创建隐形盔甲架
        val armorStand = createSeatEntity(seatLocation) ?: return false

        // 让玩家骑乘盔甲架
        armorStand.addPassenger(player)

        // 创建座位对象
        val seat = ChairSeat(
            player = player,
            block = block,
            seatLocation = seatLocation,
            armorStand = armorStand,
            originalLocation = player.location.clone()
        )

        // 存储座位信息
        seats[player.uniqueId] = seat
        blockSeats.computeIfAbsent(block) { mutableSetOf() }.add(seat)

        // 发送坐下消息
        MessageUtil.sendMessage(player, "chair.sit_success")
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: Player ${player.name} sat on block at ${block.location}")
        }

        return true
    }

    /**
     * 让玩家起身
     */
    fun standUpPlayer(player: Player): Boolean {
        val seat = seats[player.uniqueId] ?: return false

        // 移除乘客关系
        seat.armorStand.removePassenger(player)
        
        // 删除盔甲架
        seat.armorStand.remove()

        // 传送玩家到原位置或安全位置
        val returnLocation = if (plugin.getConfigManager().getConfig().getBoolean("modules.chair.return-to-original", false)) {
            seat.originalLocation
        } else {
            calculateStandUpLocation(seat)
        }
        
        player.teleport(returnLocation)

        // 移除座位信息
        seats.remove(player.uniqueId)
        blockSeats[seat.block]?.remove(seat)
        if (blockSeats[seat.block]?.isEmpty() == true) {
            blockSeats.remove(seat.block)
        }

        // 发送起身消息
        MessageUtil.sendMessage(player, "chair.stand_success")
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: Player ${player.name} stood up from chair")
        }

        return true
    }

    /**
     * 计算座位位置
     */
    private fun calculateSeatLocation(block: Block, player: Player): Location {
        val location = block.location.clone().add(0.5, 0.0, 0.5)
        
        // 根据方块类型调整高度
        val heightOffset = when (block.type.name) {
            "ACACIA_STAIRS", "BIRCH_STAIRS", "DARK_OAK_STAIRS",
            "JUNGLE_STAIRS", "OAK_STAIRS", "SPRUCE_STAIRS",
            "BRICK_STAIRS", "STONE_BRICK_STAIRS",
            "NETHER_BRICK_STAIRS", "SANDSTONE_STAIRS", "QUARTZ_STAIRS" -> -0.5

            "ACACIA_SLAB", "BIRCH_SLAB", "DARK_OAK_SLAB",
            "JUNGLE_SLAB", "OAK_SLAB", "SPRUCE_SLAB",
            "STONE_SLAB", "STONE_BRICK_SLAB" -> -0.5

            else -> -0.4 // 默认高度
        }
        
        location.y += heightOffset
        location.yaw = player.location.yaw
        location.pitch = player.location.pitch
        
        return location
    }

    /**
     * 计算起身位置
     */
    private fun calculateStandUpLocation(seat: ChairSeat): Location {
        val location = seat.seatLocation.clone()
        location.y = seat.block.location.y + 1.0
        return location
    }

    /**
     * 创建座位实体（隐形盔甲架）
     */
    private fun createSeatEntity(location: Location): ArmorStand? {
        val world = location.world ?: return null
        
        val armorStand = world.spawnEntity(location, EntityType.ARMOR_STAND) as ArmorStand
        
        // 设置盔甲架属性
        armorStand.isVisible = false
        armorStand.isSmall = true
        armorStand.setGravity(false)
        armorStand.isInvulnerable = true
        armorStand.setCanPickupItems(false)
        armorStand.customName = "PostBits-Chair"
        armorStand.isCustomNameVisible = false
        
        return armorStand
    }

    /**
     * 清理所有座位（插件禁用时调用）
     */
    fun cleanup() {
        seats.values.forEach { seat ->
            seat.armorStand.removePassenger(seat.player)
            seat.armorStand.remove()
            seat.player.teleport(seat.originalLocation)
        }
        seats.clear()
        blockSeats.clear()
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: Chair service cleaned up")
        }
    }

    /**
     * 获取所有座位数量
     */
    fun getSeatCount(): Int {
        return seats.size
    }

    /**
     * 检查方块是否可以坐
     */
    fun isSittableBlock(block: Block): Boolean {
        val config = plugin.getConfigManager().getConfig()
        val sittableBlocks = config.getStringList("modules.chair.sittable-blocks")
        
        return sittableBlocks.contains(block.type.name.lowercase()) || 
               sittableBlocks.contains("*")
    }
}
