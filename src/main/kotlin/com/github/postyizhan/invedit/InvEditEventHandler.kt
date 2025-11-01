package com.github.postyizhan.invedit

import com.github.postyizhan.PostBits
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.*

/**
 * 背包编辑功能事件监听器 - 事件驱动实时同步版本
 * 监听目标玩家和编辑者的各种背包变化事件，实现即时同步
 *
 * @author postyizhan
 */
class InvEditEventHandler(
    private val plugin: PostBits,
    private val invEditService: InvEditService
) : Listener {
    
    /**
     * 处理编辑者GUI点击事件
     * 1. 限制只能编辑物品槽位，不能点击分隔符
     * 2. 点击后立即同步到目标玩家
     */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        
        // 检查是否是编辑者
        if (invEditService.isEditing(player)) {
            handleEditorClick(event, player)
            return
        }
        
        // 检查是否是被编辑者
        if (invEditService.isBeingEdited(player)) {
            handleTargetClick(event, player)
        }
    }
    
    /**
     * 处理编辑者在GUI中的点击
     */
    private fun handleEditorClick(event: InventoryClickEvent, editor: Player) {
        val clickedInv = event.clickedInventory ?: return
        val topInv = event.view.topInventory
        
        // 只处理点击编辑GUI的情况
        if (clickedInv != topInv) {
            return
        }
        
        val slot = event.slot
        
        // 允许编辑的槽位：0-41（背包、装备、主副手）
        // 禁止操作的槽位：42-53（分隔符）
        if (slot in 42..53) {
            event.isCancelled = true
            
            // 如果点击的是分隔符，阻止拿取
            val clickedItem = event.currentItem
            if (clickedItem?.type == Material.GRAY_STAINED_GLASS_PANE) {
                if (plugin.isDebugEnabled()) {
                    plugin.logger.info("Debug: ${editor.name} tried to click separator at slot $slot")
                }
            }
            return
        }
        
        // 槽位 0-41 允许正常操作，下一tick同步到目标玩家
        Bukkit.getScheduler().runTask(plugin, Runnable {
            invEditService.onEditorGuiChanged(editor)
        })
    }
    
    /**
     * 处理被编辑者在自己背包中的点击
     */
    @Suppress("UNUSED_PARAMETER")
    private fun handleTargetClick(event: InventoryClickEvent, target: Player) {
        // 下一tick同步到所有编辑者的GUI
        Bukkit.getScheduler().runTask(plugin, Runnable {
            invEditService.onTargetInventoryChanged(target)
        })
    }
    
    /**
     * 处理编辑者的拖拽事件
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInventoryDrag(event: InventoryDragEvent) {
        val player = event.whoClicked as? Player ?: return
        
        if (invEditService.isEditing(player)) {
            // 编辑者拖拽，同步到目标玩家
            Bukkit.getScheduler().runTask(plugin, Runnable {
                invEditService.onEditorGuiChanged(player)
            })
        } else if (invEditService.isBeingEdited(player)) {
            // 被编辑者操作，同步到编辑者
            Bukkit.getScheduler().runTask(plugin, Runnable {
                invEditService.onTargetInventoryChanged(player)
            })
        }
    }
    
    /**
     * 处理目标玩家捡起物品
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityPickupItem(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return
        
        if (invEditService.isBeingEdited(player)) {
            Bukkit.getScheduler().runTask(plugin, Runnable {
                invEditService.onTargetInventoryChanged(player)
            })
        }
    }
    
    /**
     * 处理目标玩家丢弃物品
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        val player = event.player
        
        if (invEditService.isBeingEdited(player)) {
            Bukkit.getScheduler().runTask(plugin, Runnable {
                invEditService.onTargetInventoryChanged(player)
            })
        }
    }
    
    /**
     * 处理目标玩家切换主副手
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerSwapHandItems(event: PlayerSwapHandItemsEvent) {
        val player = event.player
        
        if (invEditService.isBeingEdited(player)) {
            Bukkit.getScheduler().runTask(plugin, Runnable {
                invEditService.onTargetInventoryChanged(player)
            })
        }
    }
    
    /**
     * 处理目标玩家切换手持物品栏
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerItemHeld(event: PlayerItemHeldEvent) {
        val player = event.player
        
        if (invEditService.isBeingEdited(player)) {
            // 切换快捷栏会改变主手物品
            Bukkit.getScheduler().runTask(plugin, Runnable {
                invEditService.onTargetInventoryChanged(player)
            })
        }
    }
    
    /**
     * 处理背包关闭事件
     * 当编辑者关闭GUI时，清理编辑会话
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        
        // 检查是否正在编辑
        if (invEditService.isEditing(player)) {
            // 清理编辑会话
            invEditService.closeEditingSession(player)
            
            if (plugin.isDebugEnabled()) {
                val target = invEditService.getEditingTarget(player)
                plugin.logger.info("Debug: ${player.name} closed inventory editor${if (target != null) " for ${target.name}" else ""}")
            }
        }
    }
    
    /**
     * 处理玩家退出事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        invEditService.onPlayerQuit(event.player)
    }
}

