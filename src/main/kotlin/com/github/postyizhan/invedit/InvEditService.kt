package com.github.postyizhan.invedit

import com.github.postyizhan.PostBits
import com.github.postyizhan.util.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.util.*

/**
 * 背包编辑服务类
 * 负责管理背包编辑功能的核心逻辑
 *
 * @author postyizhan
 */
class InvEditService(private val plugin: PostBits) {

    private val editingSessions = mutableMapOf<UUID, Player>() // 编辑者 -> 被编辑者
    private val editingGuis = mutableMapOf<UUID, Inventory>() // 编辑者 -> GUI界面

    /**
     * 打开玩家背包编辑界面
     */
    fun openPlayerInventory(editor: Player, target: Player): Boolean {
        // 检查是否尝试编辑自己
        if (editor == target) {
            MessageUtil.sendMessage(editor, "invedit.cannot_edit_self")
            return false
        }
        
        // 创建GUI界面
        val gui = createInventoryGUI(target)
        
        // 记录编辑会话
        editingSessions[editor.uniqueId] = target
        editingGuis[editor.uniqueId] = gui
        
        // 打开GUI
        editor.openInventory(gui)
        
        MessageUtil.sendMessage(editor, "invedit.opened_inventory", "{player}" to target.name)
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: ${editor.name} opened inventory editor for ${target.name}")
        }
        
        return true
    }
    
    /**
     * 创建背包编辑GUI
     */
    private fun createInventoryGUI(target: Player): Inventory {
        val title = MessageUtil.getMessage("invedit.gui_title").replace("{player}", target.name)
        val gui = Bukkit.createInventory(null, 54, MessageUtil.color(title))
        
        // 复制目标玩家的背包内容 (0-35: 背包, 36-39: 盔甲)
        val targetInventory = target.inventory.contents
        val targetArmor = target.inventory.armorContents
        
        // 放置背包物品 (前36个槽位)
        for (i in 0 until 36) {
            if (i < targetInventory.size) {
                gui.setItem(i, targetInventory[i])
            }
        }
        
        // 放置盔甲物品 (36-39槽位)
        for (i in 0 until 4) {
            if (i < targetArmor.size) {
                gui.setItem(36 + i, targetArmor[i])
            }
        }
        
        // 放置副手物品 (40槽位)
        gui.setItem(40, target.inventory.itemInOffHand)
        
        // 添加分隔符和说明
        addGuiDecorations(gui)
        
        return gui
    }
    
    /**
     * 添加GUI装饰和说明
     */
    private fun addGuiDecorations(gui: Inventory) {
        // 创建分隔符
        val separator = ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(MessageUtil.color("&7分隔符"))
            }
        }
        
        // 放置分隔符 (41-44槽位)
        for (i in 41..44) {
            gui.setItem(i, separator)
        }
        
        // 创建说明物品
        val info = ItemStack(Material.BOOK).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(MessageUtil.color("&6背包编辑说明"))
                lore = listOf(
                    MessageUtil.color("&7槽位 0-35: 背包物品"),
                    MessageUtil.color("&7槽位 36: 靴子"),
                    MessageUtil.color("&7槽位 37: 护腿"),
                    MessageUtil.color("&7槽位 38: 胸甲"),
                    MessageUtil.color("&7槽位 39: 头盔"),
                    MessageUtil.color("&7槽位 40: 副手"),
                    MessageUtil.color("&e点击保存按钮应用更改")
                )
            }
        }
        gui.setItem(45, info)
        
        // 创建保存按钮
        val saveButton = ItemStack(Material.EMERALD_BLOCK).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(MessageUtil.color("&a保存更改"))
                lore = listOf(
                    MessageUtil.color("&7点击保存对背包的修改")
                )
            }
        }
        gui.setItem(49, saveButton)
        
        // 创建取消按钮
        val cancelButton = ItemStack(Material.REDSTONE_BLOCK).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(MessageUtil.color("&c取消编辑"))
                lore = listOf(
                    MessageUtil.color("&7关闭界面而不保存更改")
                )
            }
        }
        gui.setItem(53, cancelButton)
    }
    
    /**
     * 保存背包更改
     */
    fun saveInventoryChanges(editor: Player): Boolean {
        val target = editingSessions[editor.uniqueId]
        val gui = editingGuis[editor.uniqueId]
        
        if (target == null || gui == null) {
            return false
        }
        
        // 检查目标玩家是否仍在线
        if (!target.isOnline) {
            MessageUtil.sendMessage(editor, "invedit.target_offline")
            return false
        }
        
        // 保存背包内容 (0-35)
        val newInventory = arrayOfNulls<ItemStack>(36)
        for (i in 0 until 36) {
            newInventory[i] = gui.getItem(i)
        }
        target.inventory.contents = newInventory
        
        // 保存盔甲内容 (36-39)
        val newArmor = arrayOfNulls<ItemStack>(4)
        for (i in 0 until 4) {
            newArmor[i] = gui.getItem(36 + i)
        }
        target.inventory.armorContents = newArmor
        
        // 保存副手物品 (40)
        target.inventory.setItemInOffHand(gui.getItem(40))
        
        // 更新玩家显示
        target.updateInventory()
        
        MessageUtil.sendMessage(editor, "invedit.changes_saved", "{player}" to target.name)
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: ${editor.name} saved inventory changes for ${target.name}")
        }
        
        return true
    }
    
    /**
     * 关闭编辑会话
     */
    fun closeEditingSession(editor: Player) {
        editingSessions.remove(editor.uniqueId)
        editingGuis.remove(editor.uniqueId)
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: Closed editing session for ${editor.name}")
        }
    }
    
    /**
     * 检查玩家是否正在编辑背包
     */
    fun isEditing(editor: Player): Boolean {
        return editingSessions.containsKey(editor.uniqueId)
    }
    
    /**
     * 获取正在编辑的目标玩家
     */
    fun getEditingTarget(editor: Player): Player? {
        return editingSessions[editor.uniqueId]
    }
    
    /**
     * 获取编辑GUI
     */
    fun getEditingGui(editor: Player): Inventory? {
        return editingGuis[editor.uniqueId]
    }
    
    /**
     * 玩家下线时清理
     */
    fun onPlayerQuit(player: Player) {
        // 如果是编辑者下线，清理会话
        closeEditingSession(player)
        
        // 如果是被编辑者下线，通知所有编辑者
        val editorsToRemove = mutableListOf<UUID>()
        for ((editorId, target) in editingSessions) {
            if (target == player) {
                val editor = plugin.server.getPlayer(editorId)
                if (editor != null) {
                    editor.closeInventory()
                    MessageUtil.sendMessage(editor, "invedit.target_offline")
                }
                editorsToRemove.add(editorId)
            }
        }
        
        // 清理会话
        editorsToRemove.forEach { editorId ->
            editingSessions.remove(editorId)
            editingGuis.remove(editorId)
        }
    }
    
    /**
     * 清理所有编辑会话
     */
    fun cleanup() {
        editingSessions.clear()
        editingGuis.clear()
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: InvEdit service cleaned up")
        }
    }
}
