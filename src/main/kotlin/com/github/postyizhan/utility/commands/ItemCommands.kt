package com.github.postyizhan.utility.commands

import com.github.postyizhan.PostBits
import com.github.postyizhan.util.MessageUtil
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable

/**
 * 物品命令处理器
 * 包含：/fix, /hat
 * 
 * @author postyizhan
 */
class ItemCommands(private val plugin: PostBits) {

    /**
     * 处理 /fix 命令
     */
    fun handleFix(sender: CommandSender, @Suppress("UNUSED_PARAMETER") args: Array<String>): Boolean {
        if (sender !is Player) {
            MessageUtil.sendMessage(sender, "messages.player_only")
            return true
        }

        if (!sender.hasPermission("postbits.utility.fix")) {
            MessageUtil.sendMessage(sender, "messages.no_permission")
            return true
        }

        val item = sender.inventory.itemInMainHand
        
        // 检查是否有物品
        if (item.type == Material.AIR) {
            MessageUtil.sendMessage(sender, "utility.fix_no_item")
            return false
        }
        
        // 检查物品类型是否有耐久度（最大耐久度大于 0）
        if (item.type.maxDurability <= 0) {
            MessageUtil.sendMessage(sender, "utility.fix_not_damageable")
            return false
        }
        
        // 检查物品元数据
        val meta = item.itemMeta
        if (meta !is Damageable) {
            MessageUtil.sendMessage(sender, "utility.fix_not_damageable")
            return false
        }
        
        // 检查是否需要修复
        if (meta.damage == 0) {
            MessageUtil.sendMessage(sender, "utility.fix_already_full")
            return false
        }
        
        // 修复物品
        meta.damage = 0
        item.itemMeta = meta
        
        MessageUtil.sendMessage(sender, "utility.fix_success")
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: Player ${sender.name} fixed item ${item.type}")
        }
        
        return true
    }

    /**
     * 处理 /hat 命令
     */
    fun handleHat(sender: CommandSender, args: Array<String>): Boolean {
        if (sender !is Player) {
            MessageUtil.sendMessage(sender, "messages.player_only")
            return true
        }

        if (!sender.hasPermission("postbits.utility.hat")) {
            MessageUtil.sendMessage(sender, "messages.no_permission")
            return true
        }

        when {
            args.isEmpty() -> {
                wearItemAsHat(sender)
            }
            
            args[0].equals("remove", ignoreCase = true) -> {
                removeHat(sender)
            }
            
            else -> {
                MessageUtil.sendMessage(sender, "utility.hat_help")
            }
        }
        return true
    }

    /**
     * 将手上的物品戴在头上
     */
    private fun wearItemAsHat(player: Player): Boolean {
        val inventory = player.inventory
        val handItem = inventory.itemInMainHand

        // 检查手上是否有物品
        if (handItem.type == Material.AIR) {
            MessageUtil.sendMessage(player, "utility.hat_no_item")
            return false
        }

        // 获取当前头盔
        val currentHelmet = inventory.helmet

        // 交换物品
        inventory.helmet = handItem
        inventory.setItemInMainHand(currentHelmet ?: ItemStack(Material.AIR))
        
        MessageUtil.sendMessage(player, "utility.hat_success")
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: Player ${player.name} wore ${handItem.type} as hat")
        }
        
        return true
    }

    /**
     * 取下头上的物品
     */
    private fun removeHat(player: Player): Boolean {
        val inventory = player.inventory
        val helmet = inventory.helmet

        // 检查头上是否有物品
        if (helmet == null || helmet.type == Material.AIR) {
            MessageUtil.sendMessage(player, "utility.hat_no_helmet")
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
        MessageUtil.sendMessage(player, "utility.hat_removed")
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: Player ${player.name} removed hat")
        }
        
        return true
    }

    /**
     * Tab 补全
     */
    fun onTabComplete(args: Array<out String>): List<String> {
        if (args.size == 1 && "remove".startsWith(args[0].lowercase())) {
            return listOf("remove")
        }
        return emptyList()
    }
}

