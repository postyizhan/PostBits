package com.github.postyizhan.shulkerbox

import com.github.postyizhan.PostBits
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * 潜影盒快开事件监听器
 * 
 * @author postyizhan
 */
class ShulkerBoxEventHandler(
    private val plugin: PostBits,
    private val shulkerBoxService: ShulkerBoxService
) : Listener {
    
    /**
     * 处理玩家右键事件
     */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        
        // 只处理右键空气
        if (event.action != Action.RIGHT_CLICK_AIR) {
            return
        }
        
        // 检查权限
        if (!player.hasPermission("postbits.shulkerbox.use")) {
            return
        }
        
        // 获取手持物品和手的位置
        val item = event.item ?: return
        val hand = event.hand ?: return
        
        // 检查是否是潜影盒
        if (!shulkerBoxService.isShulkerBox(item)) {
            return
        }
        
        // 打开潜影盒
        if (shulkerBoxService.openShulkerBox(player, item, hand)) {
            event.isCancelled = true
            
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: ${player.name} quick-opened shulker box from $hand")
            }
        }
    }
    
    /**
     * 处理背包关闭事件 - 保存潜影盒内容
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        val inventory = event.inventory
        
        // 检查是否是被追踪的潜影盒
        if (shulkerBoxService.isTrackedShulkerBox(inventory)) {
            // 保存潜影盒内容
            shulkerBoxService.saveAndCloseShulkerBox(player, inventory)
            
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Debug: ${player.name} closed shulker box inventory")
            }
        }
    }
    
    /**
     * 处理玩家退出事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        shulkerBoxService.onPlayerQuit(event.player)
    }
}

