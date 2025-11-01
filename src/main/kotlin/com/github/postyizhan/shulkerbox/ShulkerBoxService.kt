package com.github.postyizhan.shulkerbox

import com.github.postyizhan.PostBits
import org.bukkit.Material
import org.bukkit.block.ShulkerBox
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 潜影盒快开服务类
 * 
 * @author postyizhan
 */
class ShulkerBoxService(private val plugin: PostBits) {

    // 玩家打开的潜影盒会话记录
    private data class ShulkerBoxSession(
        val player: Player,
        val hand: EquipmentSlot,  // 主手还是副手
        val inventory: Inventory,  // 潜影盒的inventory
        val originalItem: ItemStack  // 原始物品的副本
    )
    
    // 记录：Inventory -> Session
    private val openedShulkerBoxes = ConcurrentHashMap<Inventory, ShulkerBoxSession>()

    /**
     * 检查物品是否是潜影盒
     */
    fun isShulkerBox(item: ItemStack?): Boolean {
        if (item == null || item.type == Material.AIR) {
            return false
        }
        
        return item.type.name.contains("SHULKER_BOX")
    }
    
    /**
     * 打开手持的潜影盒
     */
    fun openShulkerBox(player: Player, item: ItemStack, hand: EquipmentSlot): Boolean {
        if (!isShulkerBox(item)) {
            return false
        }
        
        // 获取潜影盒的 BlockStateMeta
        val meta = item.itemMeta ?: return false
        if (meta !is BlockStateMeta) {
            return false
        }
        
        val blockState = meta.blockState
        if (blockState !is ShulkerBox) {
            return false
        }
        
        // 打开潜影盒的背包界面
        val inventory = blockState.inventory
        player.openInventory(inventory)
        
        // 记录会话信息
        val session = ShulkerBoxSession(
            player = player,
            hand = hand,
            inventory = inventory,
            originalItem = item.clone()
        )
        openedShulkerBoxes[inventory] = session
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: ${player.name} opened shulker box from $hand: ${item.type}")
        }
        
        return true
    }
    
    /**
     * 保存潜影盒内容并关闭
     */
    fun saveAndCloseShulkerBox(player: Player, inventory: Inventory) {
        val session = openedShulkerBoxes.remove(inventory) ?: return
        
        // 检查玩家是否匹配
        if (session.player.uniqueId != player.uniqueId) {
            return
        }
        
        // 获取玩家当前手中的物品
        val currentItem = when (session.hand) {
            EquipmentSlot.HAND -> player.inventory.itemInMainHand
            EquipmentSlot.OFF_HAND -> player.inventory.itemInOffHand
            else -> null
        } ?: return
        
        // 确认还是同一个潜影盒（类型、名称等基本信息相同）
        if (!isSameShulkerBox(currentItem, session.originalItem)) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.warning("Debug: ${player.name}'s shulker box changed or was removed")
            }
            return
        }
        
        // 保存inventory内容到ItemStack
        val updatedItem = saveInventoryToItem(currentItem, inventory)
        
        // 更新玩家手中的物品
        when (session.hand) {
            EquipmentSlot.HAND -> player.inventory.setItemInMainHand(updatedItem)
            EquipmentSlot.OFF_HAND -> player.inventory.setItemInOffHand(updatedItem)
            else -> {}
        }
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: ${player.name} closed and saved shulker box from ${session.hand}")
        }
    }
    
    /**
     * 检查inventory是否是被追踪的潜影盒
     */
    fun isTrackedShulkerBox(inventory: Inventory): Boolean {
        return openedShulkerBoxes.containsKey(inventory)
    }
    
    /**
     * 将inventory内容保存到ItemStack
     */
    private fun saveInventoryToItem(item: ItemStack, inventory: Inventory): ItemStack {
        val meta = item.itemMeta as? BlockStateMeta ?: return item
        val blockState = meta.blockState as? ShulkerBox ?: return item
        
        // 复制inventory内容到blockState
        val shulkerInventory = blockState.inventory
        for (i in 0 until minOf(inventory.size, shulkerInventory.size)) {
            shulkerInventory.setItem(i, inventory.getItem(i))
        }
        
        // 更新blockState
        blockState.update()
        meta.blockState = blockState
        
        // 应用meta到item
        item.itemMeta = meta
        
        return item
    }
    
    /**
     * 检查两个潜影盒是否是同一个（基于类型和基本属性）
     */
    private fun isSameShulkerBox(item1: ItemStack, item2: ItemStack): Boolean {
        if (item1.type != item2.type) {
            return false
        }
        
        // 检查是否都是潜影盒
        if (!isShulkerBox(item1) || !isShulkerBox(item2)) {
            return false
        }
        
        // 比较显示名称和Lore（如果有自定义）
        val meta1 = item1.itemMeta
        val meta2 = item2.itemMeta
        
        if (meta1?.hasDisplayName() != meta2?.hasDisplayName()) {
            return false
        }
        
        if (meta1?.hasDisplayName() == true && meta1.displayName != meta2?.displayName) {
            return false
        }
        
        return true
    }
    
    /**
     * 玩家下线时清理
     */
    fun onPlayerQuit(player: Player) {
        // 移除该玩家的所有会话
        openedShulkerBoxes.entries.removeIf { it.value.player.uniqueId == player.uniqueId }
    }
    
    /**
     * 清理服务
     */
    fun cleanup() {
        openedShulkerBoxes.clear()
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: ShulkerBox service cleaned up")
        }
    }
}

