package com.github.postyizhan.invedit

import com.github.postyizhan.PostBits
import com.github.postyizhan.util.MessageUtil
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

/**
 * 背包编辑功能事件监听器
 * 处理背包编辑GUI的所有交互事件
 *
 * @author postyizhan
 */
class InvEditEventHandler(
    private val plugin: PostBits,
    private val invEditService: InvEditService
) : Listener {
    
    /**
     * 处理GUI点击事件
     */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        
        // 检查是否是背包编辑GUI
        if (!invEditService.isEditing(player)) {
            return
        }
        
        val clickedInventory = event.clickedInventory
        val editingGui = invEditService.getEditingGui(player)
        
        // 如果点击的不是编辑GUI，允许正常操作
        if (clickedInventory == null || clickedInventory != editingGui) {
            return
        }
        
        val slot = event.slot
        val clickedItem = event.currentItem
        
        // 处理特殊按钮点击
        if (clickedItem != null && clickedItem.type != Material.AIR) {
            when {
                // 保存按钮 (槽位49)
                slot == 49 && clickedItem.type == Material.EMERALD_BLOCK -> {
                    event.isCancelled = true
                    handleSaveClick(player)
                    return
                }
                
                // 取消按钮 (槽位53)
                slot == 53 && clickedItem.type == Material.REDSTONE_BLOCK -> {
                    event.isCancelled = true
                    handleCancelClick(player)
                    return
                }
                
                // 说明物品 (槽位45)
                slot == 45 && clickedItem.type == Material.BOOK -> {
                    event.isCancelled = true
                    return
                }
                
                // 分隔符 (槽位41-44)
                slot in 41..44 && clickedItem.type == Material.GRAY_STAINED_GLASS_PANE -> {
                    event.isCancelled = true
                    return
                }
            }
        }
        
        // 对于可编辑的槽位 (0-40)，允许正常编辑
        if (slot in 0..40) {
            // 这些槽位可以正常编辑，不取消事件
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: ${player.name} modified slot $slot in inventory editor")
            }
            return
        }
        
        // 其他槽位禁止操作
        event.isCancelled = true
    }
    
    /**
     * 处理保存按钮点击
     */
    private fun handleSaveClick(player: Player) {
        if (invEditService.saveInventoryChanges(player)) {
            val target = invEditService.getEditingTarget(player)
            if (target != null) {
                MessageUtil.sendMessage(player, "invedit.changes_saved", "{player}" to target.name)
            }
        } else {
            MessageUtil.sendMessage(player, "invedit.save_failed")
        }
        
        player.closeInventory()
    }
    
    /**
     * 处理取消按钮点击
     */
    private fun handleCancelClick(player: Player) {
        MessageUtil.sendMessage(player, "invedit.changes_cancelled")
        player.closeInventory()
    }
    
    /**
     * 处理GUI关闭事件
     */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        
        // 检查是否是背包编辑GUI
        if (invEditService.isEditing(player)) {
            // 清理编辑会话
            invEditService.closeEditingSession(player)
            
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: ${player.name} closed inventory editor")
            }
        }
    }
    
    /**
     * 处理玩家退出事件
     */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        invEditService.onPlayerQuit(event.player)
    }
}

