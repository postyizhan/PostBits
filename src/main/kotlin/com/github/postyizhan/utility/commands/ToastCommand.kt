package com.github.postyizhan.utility.commands

import com.github.postyizhan.PostBits
import com.github.postyizhan.util.MessageUtil
import me.anemys.anecustomtoast.ToastManager
import me.anemys.anecustomtoast.ToastType
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * Toast 通知命令
 * 用法: /toast <玩家名|all> <图标> <类型> <消息>
 *
 * @author postyizhan
 */
class ToastCommand(private val plugin: PostBits) : CommandExecutor, TabCompleter {

    private val toastManager = ToastManager(plugin)

    /**
     * 执行 toast 命令
     */
    fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        // 检查权限
        if (!sender.hasPermission("postbits.utility.toast")) {
            MessageUtil.sendMessage(sender, "messages.no_permission")
            return true
        }

        // 检查参数数量
        if (args.size < 4) {
            MessageUtil.sendMessage(sender, "utility.toast_help")
            return true
        }

        val target = args[0]
        val icon = args[1]
        val typeStr = args[2].uppercase()
        val message = args.copyOfRange(3, args.size).joinToString(" ")

        // 解析 Toast 类型
        val toastType = try {
            ToastType.valueOf(typeStr)
        } catch (e: IllegalArgumentException) {
            MessageUtil.sendMessage(sender, "utility.toast_invalid_type")
            return true
        }

        // 发送 Toast 通知
        if (target.equals("all", ignoreCase = true)) {
            // 发送给所有玩家
            if (!sender.hasPermission("postbits.utility.toast.all")) {
                MessageUtil.sendMessage(sender, "messages.no_permission")
                return true
            }

            toastManager.createToast()
                .withIcon(icon)
                .withMessage(message)
                .withStyle(toastType)
                .toAll()
                .show()
        } else {
            // 发送给指定玩家
            val targetPlayer = Bukkit.getPlayer(target)
            if (targetPlayer == null) {
                MessageUtil.sendMessage(sender, "utility.toast_player_not_found", "player" to target)
                return true
            }

            toastManager.createToast()
                .withIcon(icon)
                .withMessage(message)
                .withStyle(toastType)
                .to(targetPlayer)
                .show()
        }

        return true
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        return execute(sender, args)
    }

    /**
     * Tab 补全（用于 UtilityCommand 调用）
     */
    fun onTabComplete(args: Array<out String>): List<String> {
        return getTabCompletions(null, args)
    }

    override fun onTabComplete(sender: CommandSender?, command: Command?, alias: String, args: Array<out String>): List<String> {
        return getTabCompletions(sender, args)
    }

    /**
     * 获取 Tab 补全列表
     */
    private fun getTabCompletions(sender: CommandSender?, args: Array<out String>): List<String> {
        if (sender != null && !sender.hasPermission("postbits.utility.toast")) {
            return emptyList()
        }

        return when (args.size) {
            1 -> {
                // 第一个参数：玩家名或 all
                val players = Bukkit.getOnlinePlayers().map { it.name }.toMutableList()
                if (sender == null || sender.hasPermission("postbits.utility.toast.all")) {
                    players.add("all")
                }
                players.filter { it.startsWith(args[0], ignoreCase = true) }
            }
            2 -> {
                // 第二个参数：物品图标
                listOf("diamond", "emerald", "paper", "gold_ingot", "iron_ingot",
                    "redstone", "lapis_lazuli", "heart_of_the_sea", "nether_star")
                    .filter { it.startsWith(args[1], ignoreCase = true) }
            }
            3 -> {
                // 第三个参数：Toast 类型
                listOf("TASK", "GOAL", "CHALLENGE")
                    .filter { it.startsWith(args[2], ignoreCase = true) }
            }
            else -> emptyList()
        }
    }
}

