package com.github.postyizhan.invedit

import com.github.postyizhan.PostBits
import com.github.postyizhan.util.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 背包编辑服务类
 *
 * @author postyizhan
 */
class InvEditService(private val plugin: PostBits) {

    // 编辑会话数据
    data class EditSession(
        val editor: Player,           // 编辑者
        val target: Player,           // 被编辑的玩家
        val gui: Inventory            // 编辑GUI
    )
    
    private val editingSessions = ConcurrentHashMap<UUID, EditSession>()
    
    private val targetToEditors = ConcurrentHashMap<UUID, MutableSet<UUID>>()
    
    private val guiToEditor = ConcurrentHashMap<Inventory, UUID>()

    /**
     * 打开玩家背包编辑界面
     */
    fun openPlayerInventory(editor: Player, target: Player): Boolean {
        // 检查是否尝试编辑自己
        if (editor == target) {
            MessageUtil.sendMessage(editor, "invedit.cannot_edit_self")
            return false
        }
        if (isEditing(target)) {
            MessageUtil.sendMessage(editor, "invedit.target_is_editing")
            return false
        }
        
        closeEditingSession(editor)
        
        val gui = createEditingGui(target)
        editor.openInventory(gui)
        
        val session = EditSession(editor, target, gui)
        editingSessions[editor.uniqueId] = session
        guiToEditor[gui] = editor.uniqueId
        
        targetToEditors.computeIfAbsent(target.uniqueId) { ConcurrentHashMap.newKeySet() }
            .add(editor.uniqueId)
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: ${editor.name} opened event-driven inventory editor for ${target.name}")
        }
        
        return true
    }
    
    /**
     * 创建编辑GUI
     * 布局：
     * - 第1-4行：背包物品（36格）
     * - 第5行：装备（4格）+ 主副手（2格）+ 分隔符
     */
    private fun createEditingGui(target: Player): Inventory {
        val title = MessageUtil.getMessage("invedit.gui_title")
            .replace("{player}", target.name)
        val gui = Bukkit.createInventory(null, 54, MessageUtil.color(title))
        
        // 初始化GUI内容
        syncInventoryToGui(target, gui)
        
        return gui
    }
    
    /**
     * 获取编辑者的GUI（用于事件处理）
     */
    fun getEditorGui(editor: Player): Inventory? {
        return editingSessions[editor.uniqueId]?.gui
    }
    
    /**
     * 通过GUI获取编辑者（用于事件处理）
     */
    fun getEditorByGui(gui: Inventory): Player? {
        val editorId = guiToEditor[gui] ?: return null
        return plugin.server.getPlayer(editorId)
    }
    
    /**
     * 当目标玩家的背包发生变化时，同步到所有编辑者的GUI
     * 由事件处理器调用
     */
    fun onTargetInventoryChanged(target: Player) {
        val editorIds = targetToEditors[target.uniqueId] ?: return
        
        editorIds.forEach { editorId ->
            val session = editingSessions[editorId] ?: return@forEach
            if (session.target.uniqueId == target.uniqueId) {
                syncInventoryToGui(target, session.gui)
            }
        }
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: Synced ${target.name}'s inventory to ${editorIds.size} editor(s)")
        }
    }
    
    /**
     * 当编辑者操作GUI时，同步到目标玩家的背包
     * 由事件处理器调用
     */
    fun onEditorGuiChanged(editor: Player) {
        val session = editingSessions[editor.uniqueId] ?: return
        syncGuiToInventory(session.target, session.gui)
    }
    
    /**
     * 将目标玩家的背包同步到GUI
     */
    fun syncInventoryToGui(target: Player, gui: Inventory) {
        val targetInv = target.inventory
        
        // 同步背包物品（0-35）-> GUI (0-35)
        for (i in 0 until 36) {
            val item = targetInv.getItem(i)
            gui.setItem(i, item)
        }
        
        // 同步装备（盔甲）-> GUI (36-39)
        val armor = targetInv.armorContents
        for (i in armor.indices) {
            gui.setItem(36 + i, armor[i])
        }
        
        // 同步副手 -> GUI (40)
        gui.setItem(40, targetInv.itemInOffHand)
        
        // 同步主手 -> GUI (41)
        gui.setItem(41, targetInv.itemInMainHand)
        
        // 添加装饰分隔符（42-53）
        for (i in 42 until 54) {
            if (gui.getItem(i)?.type != Material.GRAY_STAINED_GLASS_PANE) {
                gui.setItem(i, createSeparator())
            }
        }
    }
    
    /**
     * 将GUI的内容同步到目标玩家的背包
     */
    fun syncGuiToInventory(target: Player, gui: Inventory) {
        val targetInv = target.inventory
        
        // 同步背包物品 GUI (0-35) -> 背包 (0-35)
        for (i in 0 until 36) {
            val guiItem = gui.getItem(i)
            val invItem = targetInv.getItem(i)
            
            // 只在不同时才更新，避免不必要的刷新
            if (!isSameItem(guiItem, invItem)) {
                targetInv.setItem(i, guiItem)
            }
        }
        
        // 同步装备 GUI (36-39) -> 盔甲
        val newArmor = arrayOfNulls<ItemStack>(4)
        for (i in 0 until 4) {
            newArmor[i] = gui.getItem(36 + i)
        }
        
        // 检查是否需要更新装备
        val currentArmor = targetInv.armorContents
        var armorChanged = false
        for (i in 0 until 4) {
            if (!isSameItem(newArmor[i], currentArmor.getOrNull(i))) {
                armorChanged = true
                break
            }
        }
        if (armorChanged) {
            targetInv.armorContents = newArmor
        }
        
        // 同步副手 GUI (40) -> 副手
        val newOffHand = gui.getItem(40)
        if (!isSameItem(newOffHand, targetInv.itemInOffHand)) {
            targetInv.setItemInOffHand(newOffHand)
        }
        
        // 同步主手 GUI (41) -> 主手
        val newMainHand = gui.getItem(41)
        if (!isSameItem(newMainHand, targetInv.itemInMainHand)) {
            targetInv.setItemInMainHand(newMainHand)
        }
    }
    
    /**
     * 创建分隔符物品
     */
    private fun createSeparator(): ItemStack {
        return ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(MessageUtil.color("&7"))
            }
        }
    }
    
    /**
     * 比较两个物品是否相同
     */
    private fun isSameItem(item1: ItemStack?, item2: ItemStack?): Boolean {
        if (item1 == null && item2 == null) return true
        if (item1 == null || item2 == null) return false
        return item1.isSimilar(item2) && item1.amount == item2.amount
    }
    
    /**
     * 关闭编辑会话
     */
    fun closeEditingSession(editor: Player) {
        val session = editingSessions.remove(editor.uniqueId) ?: return
        
        // 从GUI映射中移除
        guiToEditor.remove(session.gui)
        
        // 从目标玩家的编辑者列表中移除
        val targetEditors = targetToEditors[session.target.uniqueId]
        if (targetEditors != null) {
            targetEditors.remove(editor.uniqueId)
            // 如果没有编辑者了，移除整个条目
            if (targetEditors.isEmpty()) {
                targetToEditors.remove(session.target.uniqueId)
            }
        }
        
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
        return editingSessions[editor.uniqueId]?.target
    }
    
    /**
     * 检查玩家的背包是否正在被编辑
     */
    fun isBeingEdited(target: Player): Boolean {
        val editors = targetToEditors[target.uniqueId]
        return editors != null && editors.isNotEmpty()
    }
    
    /**
     * 获取正在编辑指定玩家的所有编辑者
     */
    fun getEditors(target: Player): Set<Player> {
        val editorIds = targetToEditors[target.uniqueId] ?: return emptySet()
        return editorIds.mapNotNull { plugin.server.getPlayer(it) }.toSet()
    }
    
    /**
     * 玩家下线时清理
     */
    fun onPlayerQuit(player: Player) {
        // 如果是编辑者下线，清理会话
        closeEditingSession(player)
        
        // 如果是被编辑者下线，通知所有编辑者并清理会话
        val editorIds = targetToEditors.remove(player.uniqueId) ?: return
        
        editorIds.forEach { editorId ->
            val editor = plugin.server.getPlayer(editorId)
            if (editor != null && editor.isOnline) {
                // 关闭编辑者的背包界面
                editor.closeInventory()
                MessageUtil.sendMessage(editor, "invedit.target_offline")
            }
            
            // 清理编辑会话
            val session = editingSessions.remove(editorId)
            if (session != null) {
                guiToEditor.remove(session.gui)
            }
        }
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: Cleaned up editing sessions for offline player ${player.name}")
        }
    }
    
    /**
     * 清理所有编辑会话
     */
    fun cleanup() {
        editingSessions.clear()
        targetToEditors.clear()
        guiToEditor.clear()
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: InvEdit service cleaned up (event-driven sync)")
        }
    }
}
