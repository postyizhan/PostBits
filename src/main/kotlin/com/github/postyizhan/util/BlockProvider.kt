package com.github.postyizhan.util

import org.bukkit.block.Block

/**
 * 方块提供者接口
 * 用于检查和识别自定义方块
 * 
 * @author postyizhan
 */
interface BlockProvider {
    
    /**
     * 提供者名称
     */
    val providerName: String
    
    /**
     * 检查方块是否为自定义方块
     * 
     * @param block 要检查的方块
     * @return 是否为此提供者的自定义方块
     */
    fun isCustomBlock(block: Block): Boolean
    
    /**
     * 获取自定义方块的唯一标识符
     * 
     * @param block 要检查的方块
     * @return 方块标识符，格式：namespace:id（如 "craftengine:oak_chair"）
     */
    fun getBlockId(block: Block): String?
    
    /**
     * 获取方块的显示名称
     * 
     * @param block 要检查的方块
     * @return 显示名称，如果无法获取则返回 null
     */
    fun getBlockDisplayName(block: Block): String?
    
    /**
     * 检查方块是否匹配指定的 ID 模式列表
     * 
     * @param block 要检查的方块
     * @param patterns ID 模式列表（支持通配符）
     * @return 是否匹配
     */
    fun matchesBlockId(block: Block, patterns: List<String>): Boolean {
        if (!isCustomBlock(block)) {
            return false
        }
        
        val actualId = getBlockId(block) ?: return false
        
        return patterns.any { pattern ->
            matchBlockId(actualId, pattern)
        }
    }
    
    /**
     * 匹配方块 ID
     * 默认实现支持通配符：
     * - "namespace:id" - 精确匹配
     * - "*:id" - 匹配任何命名空间的指定 ID
     * - "id" - 匹配任何命名空间的指定 ID
     * - "namespace:*" - 匹配指定命名空间的所有方块
     * - "*" - 匹配所有自定义方块
     */
    fun matchBlockId(actualId: String, pattern: String): Boolean {
        // 通配符匹配所有
        if (pattern == "*") {
            return true
        }
        
        // 分解实际 ID
        val actualParts = actualId.split(":", limit = 2)
        if (actualParts.size != 2) {
            return actualId == pattern
        }
        val actualNamespace = actualParts[0]
        val actualName = actualParts[1]
        
        // 分解模式
        val patternParts = pattern.split(":", limit = 2)
        
        return when {
            // 只有名称，无命名空间（如 "oak_chair"）
            patternParts.size == 1 -> {
                actualName == pattern
            }
            // 完整格式（如 "craftengine:oak_chair" 或 "*:oak_chair"）
            else -> {
                val patternNamespace = patternParts[0]
                val patternName = patternParts[1]
                
                val namespaceMatch = patternNamespace == "*" || patternNamespace == actualNamespace
                val nameMatch = patternName == "*" || patternName == actualName
                
                namespaceMatch && nameMatch
            }
        }
    }
}
