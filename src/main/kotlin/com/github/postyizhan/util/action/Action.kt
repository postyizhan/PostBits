package com.github.postyizhan.util.action

import org.bukkit.entity.Player

/**
 * 动作接口
 * 定义所有可执行动作的基本行为
 *
 * @author postyizhan
 */
interface Action {
    /**
     * 执行动作
     *
     * @param player 触发动作的玩家
     * @return 是否执行成功
     */
    fun execute(player: Player): Boolean

    /**
     * 获取动作类型
     */
    fun getType(): String

    /**
     * 获取动作描述（用于调试）
     */
    fun getDescription(): String
}

/**
 * 消息动作 - 向玩家发送消息
 * 格式: [message] 消息内容
 */
class MessageAction(private val message: String) : Action {
    override fun execute(player: Player): Boolean {
        player.sendMessage(com.github.postyizhan.util.MessageUtil.color(message))
        return true
    }

    override fun getType(): String = "MESSAGE"
    override fun getDescription(): String = "Send message: $message"
}

/**
 * 声音动作 - 播放声音
 * 格式: [sound] SOUND_NAME[:volume:pitch]
 * 示例: [sound] ENTITY_EXPERIENCE_ORB_PICKUP:1.0:1.0
 */
class SoundAction(private val soundName: String, private val volume: Float = 1.0f, private val pitch: Float = 1.0f) : Action {
    override fun execute(player: Player): Boolean {
        return try {
            val sound = org.bukkit.Sound.valueOf(soundName.uppercase())
            player.playSound(player.location, sound, volume, pitch)
            true
        } catch (e: IllegalArgumentException) {
            player.server.logger.warning("[Action] Invalid sound name: $soundName")
            false
        }
    }

    override fun getType(): String = "SOUND"
    override fun getDescription(): String = "Play sound: $soundName (volume=$volume, pitch=$pitch)"
}

/**
 * 控制台命令动作 - 以控制台身份执行命令
 * 格式: [console] 命令内容
 */
class ConsoleCommandAction(private val command: String) : Action {
    override fun execute(player: Player): Boolean {
        val processedCommand = command.replace("{player}", player.name)
        return player.server.dispatchCommand(player.server.consoleSender, processedCommand)
    }

    override fun getType(): String = "CONSOLE"
    override fun getDescription(): String = "Execute console command: $command"
}

/**
 * OP命令动作 - 临时给予玩家OP权限执行命令
 * 格式: [op] 命令内容
 */
class OpCommandAction(private val command: String) : Action {
    override fun execute(player: Player): Boolean {
        val wasOp = player.isOp
        return try {
            if (!wasOp) {
                player.isOp = true
            }
            val processedCommand = command.replace("{player}", player.name)
            val result = player.performCommand(processedCommand)
            result
        } finally {
            if (!wasOp) {
                player.isOp = false
            }
        }
    }

    override fun getType(): String = "OP"
    override fun getDescription(): String = "Execute OP command: $command"
}

/**
 * 玩家命令动作 - 以玩家身份执行命令
 * 格式: [player] 命令内容
 */
class PlayerCommandAction(private val command: String) : Action {
    override fun execute(player: Player): Boolean {
        val processedCommand = command.replace("{player}", player.name)
        return player.performCommand(processedCommand)
    }

    override fun getType(): String = "PLAYER"
    override fun getDescription(): String = "Execute player command: $command"
}
