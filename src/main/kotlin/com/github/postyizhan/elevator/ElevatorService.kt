package com.github.postyizhan.elevator

import com.github.postyizhan.PostBits
import com.github.postyizhan.util.BlockPattern
import com.github.postyizhan.util.MessageUtil
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.Player
import java.util.*

/**
 * 电梯服务类
 * 负责管理电梯功能的核心逻辑
 *
 * @author postyizhan
 */
class ElevatorService(private val plugin: PostBits) {

    private val lastUseTime = mutableMapOf<UUID, Long>() // 防止频繁使用

    /**
     * 处理玩家跳跃电梯上升
     */
    fun handleElevatorUp(player: Player): Boolean {
        // 检查冷却时间
        if (!checkCooldown(player)) {
            return false
        }

        val standingBlock = getStandingBlock(player)
        if (standingBlock == null || !isElevatorBlock(standingBlock)) {
            return false
        }

        // 查找上方的电梯方块
        val targetLocation = findElevatorAbove(standingBlock.location)
        if (targetLocation == null) {
            MessageUtil.sendMessage(player, "elevator.no_floor_above")
            return false
        }

        // 传送玩家
        teleportPlayer(player, targetLocation, true)
        updateCooldown(player)

        return true
    }

    /**
     * 处理玩家蹲下电梯下降
     */
    fun handleElevatorDown(player: Player): Boolean {
        // 检查冷却时间
        if (!checkCooldown(player)) {
            return false
        }

        val standingBlock = getStandingBlock(player)
        if (standingBlock == null || !isElevatorBlock(standingBlock)) {
            return false
        }

        // 查找下方的电梯方块
        val targetLocation = findElevatorBelow(standingBlock.location)
        if (targetLocation == null) {
            MessageUtil.sendMessage(player, "elevator.no_floor_below")
            return false
        }

        // 传送玩家
        teleportPlayer(player, targetLocation, false)
        updateCooldown(player)

        return true
    }

    /**
     * 获取玩家站立的方块
     */
    private fun getStandingBlock(player: Player): Block? {
        val playerLoc = player.location
        val blockBelow = playerLoc.block.getRelative(0, -1, 0)

        // 检查玩家是否真的站在方块上
        if (playerLoc.y - blockBelow.y > 1.2) {
            return null
        }

        return blockBelow
    }

    /**
     * 检查方块是否是电梯方块
     */
    fun isElevatorBlock(block: Block?): Boolean {
        if (block == null) {
            return false
        }

        val config = plugin.getConfigManager().getConfig()
        val elevatorBlocks = config.getStringList("modules.elevator.elevator-blocks")
        val hookManager = plugin.getHookManager()
        
        // 解析所有配置模式
        val patterns = BlockPattern.parseList(elevatorBlocks)
        
        // 先检查是否是自定义方块（如果有方块提供者）
        if (hookManager.hasBlockProviders()) {
            val isCustom = hookManager.isCustomBlock(block)
            
            if (isCustom) {
                val blockId = hookManager.getCustomBlockId(block)
                if (plugin.isDebugEnabled()) {
                    plugin.logger.info("Debug: [Elevator] Custom block ID: $blockId")
                    plugin.logger.info("Debug: [Elevator] Elevator blocks configured: $elevatorBlocks")
                }
                
                // 提取自定义方块模式
                val customBlockPatterns = patterns.filter { it.isCustomBlock }
                
                if (customBlockPatterns.isEmpty()) {
                    if (plugin.isDebugEnabled()) {
                        plugin.logger.info("Debug: [Elevator] No custom block patterns configured")
                    }
                    return false
                }
                
                // 遍历每个模式进行匹配（只支持提供者标记格式）
                for (pattern in customBlockPatterns) {
                    // 必须指定提供者
                    if (pattern.provider == null) {
                        if (plugin.isDebugEnabled()) {
                            plugin.logger.warning("Debug: [Elevator] Custom block pattern '${pattern.blockId}' is missing provider tag! Use format: [provider] blockId")
                            plugin.logger.warning("Debug: [Elevator] Example: [ce] elevator_block, [ia] lift_platform, [ox] lift_block")
                        }
                        continue
                    }
                    
                    // 查找指定的提供者
                    val provider = hookManager.getProviderByName(pattern.provider)
                    if (provider == null) {
                        if (plugin.isDebugEnabled()) {
                            plugin.logger.info("Debug: [Elevator] Provider '${pattern.provider}' not found for pattern: $pattern")
                        }
                        continue
                    }
                    
                    if (plugin.isDebugEnabled()) {
                        plugin.logger.info("Debug: [Elevator] Checking with provider: ${provider.providerName}")
                    }
                    
                    val matched = provider.matchesBlockId(block, listOf(pattern.blockId))
                    
                    if (matched) {
                        if (plugin.isDebugEnabled()) {
                            plugin.logger.info("Debug: [Elevator] ✓ Custom elevator block matched: $blockId with pattern: $pattern")
                        }
                        return true
                    }
                }
                
                if (plugin.isDebugEnabled()) {
                    plugin.logger.info("Debug: [Elevator] ✗ Custom block $blockId does not match any configured patterns")
                }
                
                // 如果是自定义方块但没有匹配，直接返回 false
                return false
            }
        }
        
        // 检查原版方块
        val material = block.type
        
        // 提取原版方块模式（不包含冒号的）
        val vanillaPatterns = patterns.filter { !it.isCustomBlock }.map { it.blockId }

        return vanillaPatterns.any { 
            try {
                Material.valueOf(it.uppercase()) == material
            } catch (e: IllegalArgumentException) {
                false
            }
        }
    }

    /**
     * 查找上方的电梯方块
     */
    private fun findElevatorAbove(startLocation: Location): Location? {
        val config = plugin.getConfigManager().getConfig()
        val maxHeight = config.getInt("modules.elevator.max-search-height", 50)
        val minDistance = config.getInt("modules.elevator.min-floor-distance", 2)

        for (y in (startLocation.y.toInt() + minDistance)..(startLocation.y.toInt() + maxHeight)) {
            val checkLoc = Location(startLocation.world, startLocation.x, y.toDouble(), startLocation.z)
            val block = checkLoc.block

            if (isElevatorBlock(block)) {
                // 检查上方是否有足够空间
                val spaceCheckLoc = checkLoc.clone().add(0.0, 1.0, 0.0)
                if (hasEnoughSpace(spaceCheckLoc)) {
                    return spaceCheckLoc // 返回方块上方的位置
                }
            }
        }

        return null
    }

    /**
     * 查找下方的电梯方块
     */
    private fun findElevatorBelow(startLocation: Location): Location? {
        val config = plugin.getConfigManager().getConfig()
        val maxDepth = config.getInt("modules.elevator.max-search-depth", 50)
        val minDistance = config.getInt("modules.elevator.min-floor-distance", 2)

        for (y in (startLocation.y.toInt() - minDistance) downTo (startLocation.y.toInt() - maxDepth)) {
            val checkLoc = Location(startLocation.world, startLocation.x, y.toDouble(), startLocation.z)
            val block = checkLoc.block

            if (isElevatorBlock(block)) {
                // 检查上方是否有足够空间
                val spaceCheckLoc = checkLoc.clone().add(0.0, 1.0, 0.0)
                if (hasEnoughSpace(spaceCheckLoc)) {
                    return spaceCheckLoc // 返回方块上方的位置
                }
            }
        }

        return null
    }

    /**
     * 检查位置是否有足够的空间
     */
    private fun hasEnoughSpace(location: Location): Boolean {
        // 检查玩家高度的两个方块是否为空
        val block1 = location.block
        val block2 = location.clone().add(0.0, 1.0, 0.0).block

        return (block1.type == Material.AIR || !block1.type.isSolid) && 
               (block2.type == Material.AIR || !block2.type.isSolid)
    }

    /**
     * 传送玩家到目标位置
     */
    private fun teleportPlayer(player: Player, targetLocation: Location, isUp: Boolean) {
        val config = plugin.getConfigManager().getConfig()
        
        // 创建新的位置对象，确保玩家在方块中心
        val teleportLocation = targetLocation.clone()
        teleportLocation.x = teleportLocation.blockX + 0.5
        teleportLocation.z = teleportLocation.blockZ + 0.5

        // 保持玩家的朝向
        teleportLocation.yaw = player.location.yaw
        teleportLocation.pitch = player.location.pitch

        // 传送玩家
        player.teleport(teleportLocation)

        // 播放音效
        if (config.getBoolean("modules.elevator.sound-enabled", true)) {
            val soundName = if (isUp) {
                config.getString("modules.elevator.up-sound", "ENTITY_ENDERMAN_TELEPORT")
            } else {
                config.getString("modules.elevator.down-sound", "ENTITY_ENDERMAN_TELEPORT")
            }
            
            try {
                val sound = Sound.valueOf(soundName ?: "ENTITY_ENDERMAN_TELEPORT")
                val volume = config.getDouble("modules.elevator.sound-volume", 0.5).toFloat()
                val pitch = config.getDouble("modules.elevator.sound-pitch", 1.0).toFloat()
                player.playSound(targetLocation, sound, volume, pitch)
            } catch (e: IllegalArgumentException) {
                // 如果音效名称无效，使用默认音效
                player.playSound(targetLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.0f)
            }
        }

        // 发送消息
        if (config.getBoolean("modules.elevator.message-enabled", true)) {
            val messageKey = if (isUp) "elevator.elevator_up" else "elevator.elevator_down"
            MessageUtil.sendMessage(player, messageKey)
        }

        // 添加粒子效果
        if (config.getBoolean("modules.elevator.particle-enabled", true)) {
            try {
                val particleName = config.getString("modules.elevator.particle-type", "PORTAL")
                val particleType = Particle.valueOf(particleName ?: "PORTAL")
                val particleCount = config.getInt("modules.elevator.particle-count", 10)
                
                player.world.spawnParticle(
                    particleType,
                    targetLocation,
                    particleCount,
                    0.5, 0.5, 0.5,
                    0.1
                )
            } catch (e: IllegalArgumentException) {
                // 如果粒子类型无效，使用默认粒子
                player.world.spawnParticle(
                    Particle.PORTAL,
                    targetLocation,
                    10,
                    0.5, 0.5, 0.5,
                    0.1
                )
            }
        }

        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: ${player.name} used elevator (${if (isUp) "up" else "down"})")
        }
    }

    /**
     * 检查冷却时间
     */
    private fun checkCooldown(player: Player): Boolean {
        val playerId = player.uniqueId
        val currentTime = System.currentTimeMillis()
        val config = plugin.getConfigManager().getConfig()
        val cooldownTime = config.getLong("modules.elevator.cooldown-time", 500)

        if (lastUseTime.containsKey(playerId)) {
            val lastTime = lastUseTime[playerId] ?: 0
            if (currentTime - lastTime < cooldownTime) {
                return false
            }
        }

        return true
    }

    /**
     * 更新冷却时间
     */
    private fun updateCooldown(player: Player) {
        lastUseTime[player.uniqueId] = System.currentTimeMillis()
    }

    /**
     * 玩家下线时清理
     */
    fun onPlayerQuit(player: Player) {
        lastUseTime.remove(player.uniqueId)
    }

    /**
     * 清理所有数据
     */
    fun cleanup() {
        lastUseTime.clear()
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: Elevator service cleaned up")
        }
    }
}
