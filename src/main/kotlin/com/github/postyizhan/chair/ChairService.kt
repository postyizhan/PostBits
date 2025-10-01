package com.github.postyizhan.chair

import com.github.postyizhan.PostBits
import com.github.postyizhan.util.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Bisected
import org.bukkit.block.data.type.Slab
import org.bukkit.block.data.type.Stairs
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 椅子服务类 - 管理玩家坐下功能
 * 参考 GSit 的 SitService 实现
 * 
 * @author postyizhan
 */
class ChairService(private val plugin: PostBits) {

    companion object {
        // 楼梯偏移常量（参考 GSit）
        const val STAIR_XZ_OFFSET = 0.123
        const val STAIR_Y_OFFSET = 0.5
        
        // 座位实体标签
        const val SEAT_TAG = "PostBits_Chair"
    }

    // 存储所有座位信息（玩家UUID -> 座位）
    private val seats = ConcurrentHashMap<UUID, ChairSeat>()
    
    // 存储方块对应的座位（方块 -> 座位集合）
    private val blockSeats = ConcurrentHashMap<Block, MutableSet<ChairSeat>>()
    
    // 阻止实体重复操作的临时黑名单
    private val entityBlocked = Collections.synchronizedSet(mutableSetOf<UUID>())
    
    // 基础高度偏移（根据服务器版本自动调整）
    private val baseOffset: Double
    
    init {
        // 根据服务器版本计算基础偏移
        baseOffset = calculateBaseOffset()
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: ChairService initialized with baseOffset=$baseOffset")
        }
    }

    /**
     * 根据服务器版本计算基础高度偏移
     */
    private fun calculateBaseOffset(): Double {
        val version = Bukkit.getBukkitVersion()
        
        // 解析版本号，例如 "1.20.2-R0.1-SNAPSHOT"
        val versionParts = version.split("-")[0].split(".")
        if (versionParts.size >= 2) {
            val major = versionParts[0].toIntOrNull() ?: 1
            val minor = versionParts[1].toIntOrNull() ?: 20
            val patch = if (versionParts.size >= 3) versionParts[2].toIntOrNull() ?: 0 else 0
            
            // 1.20.2+ 版本使用 -0.05，之前版本使用 0.2
            return if (major > 1 || (major == 1 && minor > 20) || 
                      (major == 1 && minor == 20 && patch >= 2)) {
                -0.05
            } else {
                0.2
            }
        }
        
        // 默认值
        return 0.2
    }

    /**
     * 检查玩家是否正在坐着
     */
    fun isPlayerSitting(player: Player): Boolean {
        return seats.containsKey(player.uniqueId)
    }

    /**
     * 检查实体是否被临时阻止
     */
    fun isEntityBlocked(player: Player): Boolean {
        return entityBlocked.contains(player.uniqueId)
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
    fun sitPlayer(player: Player, block: Block, clickedX: Double = 0.0, clickedZ: Double = 0.0): Boolean {
        // 检查玩家是否已经在坐着
        if (isPlayerSitting(player)) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: Player ${player.name} is already sitting")
            }
            return false
        }
        
        // 检查玩家是否被临时阻止
        if (isEntityBlocked(player)) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: Player ${player.name} is temporarily blocked")
            }
            return false
        }

        // 检查方块是否被占用（如果不允许多人坐同一方块）
        if (!plugin.getConfigManager().getConfig().getBoolean("modules.chair.allow-multiple-players", false) 
            && isBlockOccupied(block)) {
            MessageUtil.sendMessage(player, "chair.already_occupied")
            return false
        }

        // 根据方块类型创建座位
        val seat = createSeat(block, player, clickedX, clickedZ)
        
        if (seat != null) {
            MessageUtil.sendMessage(player, "chair.sit_success")
            
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: Player ${player.name} sat on ${block.type} at ${block.location}")
            }
            return true
        }
        
        return false
    }

    /**
     * 创建座位（核心方法）
     */
    private fun createSeat(block: Block, player: Player, clickedX: Double, clickedZ: Double): ChairSeat? {
        val returnLocation = player.location.clone()
        
        // 特殊处理楼梯方块
        if (Tag.STAIRS.isTagged(block.type)) {
            val blockData = block.blockData as? Stairs
            if (blockData?.half == Bisected.Half.BOTTOM) {
                return createStairSeat(block, player, blockData)
            }
        }
        
        // 普通方块和台阶
        val centerBlock = plugin.getConfigManager().getConfig().getBoolean("modules.chair.center-block", true)
        val seatLocation = calculateSeatLocation(block, player, clickedX, clickedZ, centerBlock)
        
        // 验证座位位置是否安全
        if (!isSeatLocationValid(seatLocation)) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: Seat location is not valid for ${player.name}")
            }
            return null
        }
        
        // 创建座位实体
        val armorStand = createSeatEntity(seatLocation, player) ?: return null
        
        // 让玩家骑乘
        armorStand.addPassenger(player)
        
        // 创建座位对象
        val seat = ChairSeat(
            block = block,
            seatLocation = seatLocation,
            player = player,
            seatEntity = armorStand,
            returnLocation = returnLocation
        )
        
        // 存储座位信息
        seats[player.uniqueId] = seat
        blockSeats.computeIfAbsent(block) { ConcurrentHashMap.newKeySet() }.add(seat)
        
        return seat
    }
    
    /**
     * 创建楼梯座位（带朝向）
     */
    private fun createStairSeat(block: Block, player: Player, blockData: Stairs): ChairSeat? {
        val returnLocation = player.location.clone()
        val facing = blockData.facing.oppositeFace
        
        // 根据楼梯朝向和形状计算偏移和旋转
        val (xOffset, zOffset, yaw) = when {
            blockData.shape == Stairs.Shape.STRAIGHT -> {
                when (facing) {
                    BlockFace.EAST -> Triple(STAIR_XZ_OFFSET, 0.0, -90f)
                    BlockFace.SOUTH -> Triple(0.0, STAIR_XZ_OFFSET, 0f)
                    BlockFace.WEST -> Triple(-STAIR_XZ_OFFSET, 0.0, 90f)
                    BlockFace.NORTH -> Triple(0.0, -STAIR_XZ_OFFSET, 180f)
                    else -> Triple(0.0, 0.0, player.location.yaw)
                }
            }
            // 处理拐角楼梯
            else -> calculateCornerStairOffset(facing, blockData.shape, player)
        }
        
        val seatLocation = block.location.clone().add(
            0.5 + xOffset, 
            -baseOffset - STAIR_Y_OFFSET + getBlockHeightOffset(block),
            0.5 + zOffset
        )
        seatLocation.yaw = yaw
        seatLocation.pitch = 0f
        
        // 验证座位位置
        if (!isSeatLocationValid(seatLocation)) {
            return null
        }
        
        // 创建座位实体（楼梯不允许旋转）
        val armorStand = createSeatEntity(seatLocation, player, canRotate = false) ?: return null
        
        // 让玩家骑乘
        armorStand.addPassenger(player)
        
        // 创建座位对象
        val seat = ChairSeat(
            block = block,
            seatLocation = seatLocation,
            player = player,
            seatEntity = armorStand,
            returnLocation = returnLocation
        )
        
        // 存储座位信息
        seats[player.uniqueId] = seat
        blockSeats.computeIfAbsent(block) { ConcurrentHashMap.newKeySet() }.add(seat)
        
        return seat
    }
    
    /**
     * 计算拐角楼梯的偏移
     */
    private fun calculateCornerStairOffset(facing: BlockFace, shape: Stairs.Shape, player: Player): Triple<Double, Double, Float> {
        return when {
            // 北+右外角 或 东+左外角 或 北+右内角 或 东+左内角
            (facing == BlockFace.NORTH && shape == Stairs.Shape.OUTER_RIGHT) ||
            (facing == BlockFace.EAST && shape == Stairs.Shape.OUTER_LEFT) ||
            (facing == BlockFace.NORTH && shape == Stairs.Shape.INNER_RIGHT) ||
            (facing == BlockFace.EAST && shape == Stairs.Shape.INNER_LEFT) ->
                Triple(STAIR_XZ_OFFSET, -STAIR_XZ_OFFSET, -135f)
            
            // 北+左外角 或 西+右外角 或 北+左内角 或 西+右内角
            (facing == BlockFace.NORTH && shape == Stairs.Shape.OUTER_LEFT) ||
            (facing == BlockFace.WEST && shape == Stairs.Shape.OUTER_RIGHT) ||
            (facing == BlockFace.NORTH && shape == Stairs.Shape.INNER_LEFT) ||
            (facing == BlockFace.WEST && shape == Stairs.Shape.INNER_RIGHT) ->
                Triple(-STAIR_XZ_OFFSET, -STAIR_XZ_OFFSET, 135f)
            
            // 南+右外角 或 西+左外角 或 南+右内角 或 西+左内角
            (facing == BlockFace.SOUTH && shape == Stairs.Shape.OUTER_RIGHT) ||
            (facing == BlockFace.WEST && shape == Stairs.Shape.OUTER_LEFT) ||
            (facing == BlockFace.SOUTH && shape == Stairs.Shape.INNER_RIGHT) ||
            (facing == BlockFace.WEST && shape == Stairs.Shape.INNER_LEFT) ->
                Triple(-STAIR_XZ_OFFSET, STAIR_XZ_OFFSET, 45f)
            
            // 南+左外角 或 东+右外角 或 南+左内角 或 东+右内角
            (facing == BlockFace.SOUTH && shape == Stairs.Shape.OUTER_LEFT) ||
            (facing == BlockFace.EAST && shape == Stairs.Shape.OUTER_RIGHT) ||
            (facing == BlockFace.SOUTH && shape == Stairs.Shape.INNER_LEFT) ||
            (facing == BlockFace.EAST && shape == Stairs.Shape.INNER_RIGHT) ->
                Triple(STAIR_XZ_OFFSET, STAIR_XZ_OFFSET, -45f)
            
            else -> Triple(0.0, 0.0, player.location.yaw)
        }
    }

    /**
     * 计算座位位置
     */
    private fun calculateSeatLocation(
        block: Block,
        player: Player,
        clickedX: Double,
        clickedZ: Double,
        centerBlock: Boolean
    ): Location {
        val xOffset = if (centerBlock) 0.0 else clickedX
        val zOffset = if (centerBlock) 0.0 else clickedZ
        
        val location = if (centerBlock) {
            block.location.clone().add(
                0.5 + xOffset,
                -baseOffset + getBlockHeightOffset(block),
                0.5 + zOffset
            )
        } else {
            player.location.clone().add(
                xOffset,
                -baseOffset + getBlockHeightOffset(block),
                zOffset
            )
        }
        
        location.yaw = player.location.yaw
        location.pitch = 0f
        
        return location
    }
    
    /**
     * 获取方块高度偏移（台阶等特殊方块）
     */
    private fun getBlockHeightOffset(block: Block): Double {
        // 可以从配置文件读取自定义高度
        val config = plugin.getConfigManager().getConfig()
        val materialOffsets = mutableMapOf<Material, Double>()
        
        // 读取配置中的自定义高度（如果有）
        if (config.contains("modules.chair.block-height-offsets")) {
            val offsetsSection = config.getConfigurationSection("modules.chair.block-height-offsets")
            offsetsSection?.getKeys(false)?.forEach { key ->
                try {
                    val material = Material.valueOf(key.uppercase())
                    val offset = offsetsSection.getDouble(key)
                    materialOffsets[material] = offset
                } catch (e: IllegalArgumentException) {
                    // 忽略无效的材质名
                }
            }
        }
        
        // 优先使用配置的偏移
        if (materialOffsets.containsKey(block.type)) {
            return materialOffsets[block.type]!!
        }
        
        // 默认偏移值
        return when {
            Tag.SLABS.isTagged(block.type) -> {
                val slabData = block.blockData as? Slab
                when (slabData?.type) {
                    Slab.Type.BOTTOM -> 0.0
                    Slab.Type.TOP -> 0.5
                    Slab.Type.DOUBLE -> 0.5
                    else -> 0.0
                }
            }
            Tag.STAIRS.isTagged(block.type) -> 0.0
            else -> 0.0
        }
    }

    /**
     * 验证座位位置是否有效
     */
    private fun isSeatLocationValid(location: Location): Boolean {
        val world = location.world ?: return false
        
        // 检查位置是否在世界边界内
        if (!world.worldBorder.isInside(location)) {
            return false
        }
        
        // 从座位位置向上检测玩家头部和上方空间
        // 玩家高度约1.8格，所以检测 +1.0 和 +2.0 的位置
        // 注意：这里要检测的是玩家空间，不是座位实体空间
        val headLocation = location.clone().add(0.0, 1.0, 0.0)
        val topLocation = location.clone().add(0.0, 2.0, 0.0)
        
        val headBlock = world.getBlockAt(headLocation)
        val topBlock = world.getBlockAt(topLocation)
        
        // 检查方块是否可穿过（允许楼梯、台阶等非完整方块）
        if (!isBlockPassable(headBlock) || !isBlockPassable(topBlock)) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: Seat location blocked - head: ${headBlock.type} at ${headBlock.location}, top: ${topBlock.type} at ${topBlock.location}")
            }
            return false
        }
        
        return true
    }
    
    /**
     * 检查方块是否可穿过（兼容旧版本）
     */
    private fun isBlockPassable(block: Block): Boolean {
        val material = block.type
        
        // 空气总是可穿过
        if (material == Material.AIR) {
            return true
        }
        
        // 楼梯和台阶应该被认为是可穿过的（它们不是完整方块）
        if (Tag.STAIRS.isTagged(material) || Tag.SLABS.isTagged(material)) {
            return true
        }
        
        // 其他非固体方块也可以穿过（如火把、花、草等）
        if (!material.isSolid) {
            return true
        }
        
        return false
    }

    /**
     * 创建座位实体（隐形盔甲架）
     */
    private fun createSeatEntity(location: Location, @Suppress("UNUSED_PARAMETER") player: Player, canRotate: Boolean = true): ArmorStand? {
        val world = location.world ?: return null
        
        try {
            val armorStand = world.spawnEntity(location, EntityType.ARMOR_STAND) as ArmorStand
            
            // 设置盔甲架属性
            armorStand.isVisible = false
            armorStand.isSmall = true
            armorStand.setGravity(false)
            armorStand.isInvulnerable = true
            armorStand.setCanPickupItems(false)
            armorStand.customName = SEAT_TAG
            armorStand.isCustomNameVisible = false
            
            // 设置为非持久化（避免保存到世界文件）
            try {
                @Suppress("DEPRECATION")
                armorStand.isPersistent = false
            } catch (e: NoSuchMethodError) {
                // 旧版本不支持，忽略
            }
            
            // 标记为座位实体
            armorStand.scoreboardTags.add(SEAT_TAG)
            
            // 设置是否可旋转（设置初始朝向）
            if (!canRotate) {
                armorStand.teleport(location)
            }
            
            return armorStand
        } catch (e: Exception) {
            plugin.logger.warning("Failed to create seat entity: ${e.message}")
            if (plugin.isDebugEnabled()) {
                e.printStackTrace()
            }
            return null
        }
    }

    /**
     * 让玩家起身
     */
    fun standUpPlayer(player: Player, returnToOriginal: Boolean = false): Boolean {
        val seat = seats[player.uniqueId] ?: return false
        
        // 添加到临时黑名单
        entityBlocked.add(player.uniqueId)
        
        try {
            // 安全下座
            handleSafeDismount(seat, returnToOriginal)
            
            // 移除座位信息
            seats.remove(player.uniqueId)
            blockSeats[seat.block]?.remove(seat)
            if (blockSeats[seat.block]?.isEmpty() == true) {
                blockSeats.remove(seat.block)
            }
            
            // 删除盔甲架
            if (seat.seatEntity.isValid) {
                seat.seatEntity.remove()
            }
            
            // 发送起身消息
            MessageUtil.sendMessage(player, "chair.stand_success")
            
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: Player ${player.name} stood up (lifetime: ${seat.getLifetimeInSeconds()}s)")
            }
            
            return true
        } finally {
            // 延迟移除黑名单
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                entityBlocked.remove(player.uniqueId)
            }, 5L)
        }
    }
    
    /**
     * 安全下座处理
     */
    private fun handleSafeDismount(seat: ChairSeat, returnToOriginal: Boolean) {
        val player = seat.player
        
        if (!player.isValid || !player.isOnline) {
            return
        }
        
        try {
            // 计算返回位置
            val returnLocation = if (returnToOriginal || 
                plugin.getConfigManager().getConfig().getBoolean("modules.chair.return-to-original", false)) {
                seat.returnLocation.clone()
            } else {
                // 计算方块上方的安全位置
                val upLocation = seat.block.location.clone().add(0.5, 1.0, 0.5)
                upLocation.yaw = player.location.yaw
                upLocation.pitch = player.location.pitch
                
                // 如果是楼梯，额外偏移
                if (Tag.STAIRS.isTagged(seat.block.type)) {
                    upLocation.add(0.0, STAIR_Y_OFFSET, 0.0)
                }
                
                upLocation
            }
            
            // 传送玩家
            player.teleport(returnLocation)
            
        } catch (e: Exception) {
            plugin.logger.warning("Failed to safely dismount player ${player.name}: ${e.message}")
            if (plugin.isDebugEnabled()) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 检查方块是否可以坐
     */
    fun isSittableBlock(block: Block): Boolean {
        val config = plugin.getConfigManager().getConfig()
        val sittableBlocks = config.getStringList("modules.chair.sittable-blocks")
        
        // 检查方块名称（小写）
        val blockName = block.type.name.lowercase()
        
        // 支持通配符 *
        if (sittableBlocks.contains("*")) {
            return true
        }
        
        // 检查具体方块类型
        if (sittableBlocks.contains(blockName)) {
            return true
        }
        
        // 检查标签（如 stairs, slabs）
        if (sittableBlocks.contains("stairs") && Tag.STAIRS.isTagged(block.type)) {
            return true
        }
        
        if (sittableBlocks.contains("slabs") && Tag.SLABS.isTagged(block.type)) {
            return true
        }
        
        return false
    }

    /**
     * 清理所有座位（插件禁用时调用）
     */
    fun cleanup() {
        val seatList = seats.values.toList()
        seatList.forEach { seat ->
            try {
                seat.seatEntity.removePassenger(seat.player)
                seat.seatEntity.remove()
                seat.player.teleport(seat.returnLocation)
            } catch (e: Exception) {
                plugin.logger.warning("Error cleaning up seat for ${seat.player.name}: ${e.message}")
            }
        }
        
        seats.clear()
        blockSeats.clear()
        entityBlocked.clear()
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: Chair service cleaned up (${seatList.size} seats removed)")
        }
    }

    /**
     * 获取所有座位数量
     */
    fun getSeatCount(): Int {
        return seats.size
    }
}
