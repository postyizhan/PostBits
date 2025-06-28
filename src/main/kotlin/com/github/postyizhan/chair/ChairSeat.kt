package com.github.postyizhan.chair

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import java.util.*

/**
 * 椅子座位数据类 - 存储玩家坐下的相关信息
 * 
 * @author postyizhan
 */
data class ChairSeat(
    val player: Player,
    val block: Block,
    val seatLocation: Location,
    val armorStand: ArmorStand,
    val originalLocation: Location,
    val seatTime: Long = System.currentTimeMillis()
) {
    
    /**
     * 获取座位的唯一ID
     */
    val seatId: UUID = UUID.randomUUID()
    
    /**
     * 检查座位是否有效
     */
    fun isValid(): Boolean {
        return player.isOnline && 
               armorStand.isValid && 
               block.location.world == seatLocation.world
    }
    
    /**
     * 获取坐下时长（毫秒）
     */
    fun getSitDuration(): Long {
        return System.currentTimeMillis() - seatTime
    }
}
