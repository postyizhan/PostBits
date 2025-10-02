package com.github.postyizhan.util

/**
 * 方块模式解析器
 * 用于解析配置中的方块 ID 模式，支持 [provider] 标记语法
 * 
 * 支持的格式：
 * - "stairs" - 原版方块标签
 * - "oak_stairs" - 原版方块名称
 * - "[ce] namespace:id" - 指定提供者的自定义方块
 * - "[craftengine] namespace:id" - 完整提供者名称
 * 
 * @author postyizhan
 */
data class BlockPattern(
    val provider: String?,      // 指定的提供者（可选）
    val blockId: String,         // 方块 ID
    val isCustomBlock: Boolean   // 是否为自定义方块（包含冒号）
) {
    companion object {
        private val PATTERN_REGEX = Regex("^\\[([^\\]]+)]\\s+(.+)$")
        
        /**
         * 解析方块模式字符串
         * 
         * @param pattern 配置中的方块模式字符串
         * @return 解析后的 BlockPattern 对象
         */
        fun parse(pattern: String): BlockPattern {
            val trimmed = pattern.trim()
            
            // 尝试匹配 [provider] blockId 格式
            val match = PATTERN_REGEX.find(trimmed)
            
            return if (match != null) {
                // 有提供者标记
                val provider = match.groupValues[1].trim()
                val blockId = match.groupValues[2].trim()
                BlockPattern(
                    provider = provider,
                    blockId = blockId,
                    isCustomBlock = blockId.contains(":")
                )
            } else {
                // 没有提供者标记
                BlockPattern(
                    provider = null,
                    blockId = trimmed,
                    isCustomBlock = trimmed.contains(":")
                )
            }
        }
        
        /**
         * 批量解析方块模式列表
         * 
         * @param patterns 配置中的方块模式字符串列表
         * @return 解析后的 BlockPattern 对象列表
         */
        fun parseList(patterns: List<String>): List<BlockPattern> {
            return patterns.map { parse(it) }
        }
    }
    
    /**
     * 格式化输出（用于调试）
     */
    override fun toString(): String {
        return if (provider != null) {
            "[$provider] $blockId"
        } else {
            blockId
        }
    }
}
