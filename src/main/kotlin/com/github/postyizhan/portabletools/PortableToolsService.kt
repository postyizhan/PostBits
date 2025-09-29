package com.github.postyizhan.portabletools

import com.github.postyizhan.PostBits
import com.github.postyizhan.util.MessageUtil
import org.bukkit.entity.Player
import java.lang.reflect.Method

/**
 * 随身工具服务类
 * 
 * @author postyizhan
 */
class PortableToolsService(private val plugin: PostBits) {
    
    // 方法缓存
    private var openGrindstoneMethod: Method? = null
    private var openCartographyMethod: Method? = null
    private var openSmithingMethod: Method? = null

    fun initialize() {
        // 检测高版本API方法是否可用
        try {
            openGrindstoneMethod = Player::class.java.getMethod("openGrindstone", 
                org.bukkit.Location::class.java, Boolean::class.javaPrimitiveType)
            plugin.logger.info("砂轮API可用")
        } catch (e: NoSuchMethodException) {
            plugin.logger.info("砂轮API不可用（需要更高版本服务器）")
        }
        
        try {
            openCartographyMethod = Player::class.java.getMethod("openCartographyTable", 
                org.bukkit.Location::class.java, Boolean::class.javaPrimitiveType)
            plugin.logger.info("制图机API可用")
        } catch (e: NoSuchMethodException) {
            plugin.logger.info("制图机API不可用（需要更高版本服务器）")
        }
        
        try {
            openSmithingMethod = Player::class.java.getMethod("openSmithingTable", 
                org.bukkit.Location::class.java, Boolean::class.javaPrimitiveType)
            plugin.logger.info("锻造台API可用")
        } catch (e: NoSuchMethodException) {
            plugin.logger.info("锻造台API不可用（需要更高版本服务器）")
        }
    }

    fun cleanup() {
        // 清理方法缓存
        openGrindstoneMethod = null
        openCartographyMethod = null
        openSmithingMethod = null
    }
    /**
     * 打开便携工作台
     */
    fun openCraft(player: Player): Boolean {
        val view = player.openWorkbench(null, true)
        if (view != null) {
            MessageUtil.sendMessage(player, "portabletools.craft_opened")
            return true
        }
        return false
    }

    /**
     * 打开便携砂轮
     */
    fun openGrindstone(player: Player): Boolean {
        val method = openGrindstoneMethod
        if (method == null) {
            MessageUtil.sendMessage(player, "portabletools.not_supported")
            return false
        }
        
        return try {
            val view = method.invoke(player, null, true)
            if (view != null) {
                MessageUtil.sendMessage(player, "portabletools.grindstone_opened")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            plugin.logger.warning("打开砂轮失败: ${e.message}")
            MessageUtil.sendMessage(player, "portabletools.not_supported")
            false
        }
    }

    /**
     * 打开便携制图机
     */
    fun openCartography(player: Player): Boolean {
        val method = openCartographyMethod
        if (method == null) {
            MessageUtil.sendMessage(player, "portabletools.not_supported")
            return false
        }
        
        return try {
            val view = method.invoke(player, null, true)
            if (view != null) {
                MessageUtil.sendMessage(player, "portabletools.cartography_opened")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            plugin.logger.warning("打开制图机失败: ${e.message}")
            MessageUtil.sendMessage(player, "portabletools.not_supported")
            false
        }
    }

    /**
     * 打开便携附魔台
     */
    fun openEnchanting(player: Player): Boolean {
        val view = player.openEnchanting(null, true)
        if (view != null) {
            MessageUtil.sendMessage(player, "portabletools.enchanting_opened")
            return true
        }
        return false
    }

    /**
     * 打开便携锻造台
     */
    fun openSmithing(player: Player): Boolean {
        val method = openSmithingMethod
        if (method == null) {
            MessageUtil.sendMessage(player, "portabletools.not_supported")
            return false
        }
        
        return try {
            val view = method.invoke(player, null, true)
            if (view != null) {
                MessageUtil.sendMessage(player, "portabletools.smithing_opened")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            plugin.logger.warning("打开锻造台失败: ${e.message}")
            MessageUtil.sendMessage(player, "portabletools.not_supported")
            false
        }
    }

    /**
     * 打开便携末影箱
     */
    fun openEnderChest(player: Player): Boolean {
        val enderChest = player.enderChest
        player.openInventory(enderChest)
        MessageUtil.sendMessage(player, "portabletools.enderchest_opened")
        return true
    }
}
