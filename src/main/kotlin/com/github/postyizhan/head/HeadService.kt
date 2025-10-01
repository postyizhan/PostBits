package com.github.postyizhan.head

import com.github.postyizhan.PostBits
import com.github.postyizhan.util.MessageUtil
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * 头部装备服务类
 * 
 * @author postyizhan
 */
class HeadService(private val plugin: PostBits) {
    
    /**
     * 初始化服务
     */
    fun initialize() {
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: HeadService initialized")
        }
    }
    
    /**
     * 清理服务
     */
    fun cleanup() {
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: HeadService cleaned up")
        }
    }
    
    /**
     * 将手上的物品戴在头上
     */
    fun wearItemOnHead(player: Player): Boolean {
        val inventory = player.inventory
        val handItem = inventory.itemInMainHand

        // 检查手上是否有物品
        if (handItem.type == Material.AIR) {
            MessageUtil.sendMessage(player, "head.no_item")
            return false
        }

        // 获取当前头盔
        val currentHelmet = inventory.helmet

        // 交换物品
        inventory.helmet = handItem
        inventory.setItemInMainHand(currentHelmet ?: ItemStack(Material.AIR))
        
        MessageUtil.sendMessage(player, "head.success")
        return true
    }

    /**
     * 取下头上的物品
     */
    fun removeHeadItem(player: Player): Boolean {
        val inventory = player.inventory
        val helmet = inventory.helmet

        // 检查头上是否有物品
        if (helmet == null || helmet.type == Material.AIR) {
            MessageUtil.sendMessage(player, "head.no_helmet")
            return false
        }

        // 放到手上或背包
        val handItem = inventory.itemInMainHand
        if (handItem.type == Material.AIR) {
            inventory.setItemInMainHand(helmet)
        } else {
            inventory.addItem(helmet)
        }

        inventory.helmet = ItemStack(Material.AIR)
        MessageUtil.sendMessage(player, "head.removed")
        return true
    }
}
