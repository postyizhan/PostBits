package com.github.postyizhan.chair

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.util.*

/**
 * 椅子座位数据类 - 存储玩家坐下的相关信息
 * 参考 GSit 的 GSeat 实现
 * 
 * @author postyizhan
 */
class ChairSeat(
    var block: Block,
    var seatLocation: Location,
    val player: Player,
    val seatEntity: ArmorStand,
    var returnLocation: Location
) {
    
    // 座位生成时间（纳秒）
    private val spawnTime: Long = System.nanoTime()
    
    /**
     * 获取座位的唯一ID（基于盔甲架UUID）
     */
    val seatId: UUID
        get() = seatEntity.uniqueId
    
    /**
     * 检查座位是否有效
     */
    fun isValid(): Boolean {
        return player.isOnline && 
               player.isValid &&
               seatEntity.isValid && 
               block.world == seatLocation.world
    }
    
    /**
     * 获取座位存在时长（纳秒）
     */
    fun getLifetimeInNanoSeconds(): Long {
        return System.nanoTime() - spawnTime
    }
    
    /**
     * 获取座位存在时长（毫秒）
     */
    fun getLifetimeInMilliSeconds(): Long {
        return getLifetimeInNanoSeconds() / 1_000_000
    }
    
    /**
     * 获取座位存在时长（秒）
     */
    fun getLifetimeInSeconds(): Long {
        return getLifetimeInNanoSeconds() / 1_000_000_000
    }
    
    /**
     * 获取方块位置的副本
     */
    fun getBlockLocation(): Location {
        return block.location.clone()
    }
    
    /**
     * 获取座位位置的副本
     */
    fun getSeatLocationCopy(): Location {
        return seatLocation.clone()
    }
    
    /**
     * 获取返回位置的副本
     */
    fun getReturnLocationCopy(): Location {
        return returnLocation.clone()
    }
    
    override fun toString(): String {
        return "ChairSeat(player=${player.name}, block=${block.type}, seatId=$seatId)"
    }
}
