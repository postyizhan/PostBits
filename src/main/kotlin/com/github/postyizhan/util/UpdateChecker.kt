package com.github.postyizhan.util

import com.github.postyizhan.PostBits
import org.bukkit.Bukkit
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.regex.Pattern

class UpdateChecker(private val plugin: PostBits, private val repository: String) {

    private val currentVersion = plugin.description.version
    private var latestVersion: String? = null
    private val apiUrl = "https://api.github.com/repos/$repository/releases/latest"

    /**
     * 检查更新
     * @param callback 回调函数，参数为 (是否有更新, 最新版本)
     */
    fun checkForUpdates(callback: (Boolean, String) -> Unit) {
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Debug: Starting update check for repository: $repository")
            plugin.logger.info("Debug: Current version: $currentVersion")
            plugin.logger.info("Debug: API URL: $apiUrl")
        }

        // 在异步线程中执行
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            try {
                if (plugin.isDebugEnabled()) {
                    plugin.logger.info("Debug: Making HTTP request to GitHub API")
                }

                // 获取最新版本信息
                @Suppress("DEPRECATION")
                val connection = URL(apiUrl).openConnection()
                connection.connectTimeout = 10000  // 增加超时时间
                connection.readTimeout = 10000
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                connection.setRequestProperty("User-Agent", "PostBits UpdateChecker")

                if (plugin.isDebugEnabled()) {
                    plugin.logger.info("Debug: HTTP response code: ${connection.getHeaderField(0)}")
                }

                val reader = BufferedReader(InputStreamReader(connection.getInputStream()))
                val content = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    content.append(line)
                }
                reader.close()

                val jsonContent = content.toString()
                if (plugin.isDebugEnabled()) {
                    plugin.logger.info("Debug: Received JSON response (first 200 chars): ${jsonContent.take(200)}")
                }

                // 解析版本号
                val tagPattern = Pattern.compile("\"tag_name\"\\s*:\\s*\"(.*?)\"")
                val matcher = tagPattern.matcher(jsonContent)

                if (matcher.find()) {
                    latestVersion = matcher.group(1).replace("v", "")

                    if (plugin.isDebugEnabled()) {
                        plugin.logger.info("Debug: Latest version found: $latestVersion")
                        plugin.logger.info("Debug: Current version: $currentVersion")
                    }

                    // 在主线程中执行回调
                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        val hasUpdate = compareVersions(currentVersion, latestVersion!!) < 0
                        if (plugin.isDebugEnabled()) {
                            plugin.logger.info("Debug: Version comparison result: hasUpdate = $hasUpdate")
                            plugin.logger.info("Debug: Calling callback with hasUpdate=$hasUpdate, version=$latestVersion")
                        }
                        callback(hasUpdate, latestVersion!!)
                    })
                } else {
                    if (plugin.isDebugEnabled()) {
                        plugin.logger.info("Debug: Could not parse version from response")
                        plugin.logger.info("Debug: Full response: $jsonContent")
                    }
                    // 无法解析版本号
                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        if (plugin.isDebugEnabled()) {
                            plugin.logger.info("Debug: Calling callback with hasUpdate=false (parse failed)")
                        }
                        callback(false, currentVersion)
                    })
                }
            } catch (e: Exception) {
                // 发生异常
                plugin.logger.warning("An error occurred while checking for updates: ${e.message}")
                if (plugin.isDebugEnabled()) {
                    plugin.logger.info("Debug: Exception details:")
                    e.printStackTrace()
                }
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    if (plugin.isDebugEnabled()) {
                        plugin.logger.info("Debug: Calling callback with hasUpdate=false (exception)")
                    }
                    callback(false, currentVersion)
                })
            }
        })
    }

    /**
     * 比较版本号
     * @return 如果v1 < v2返回负数，v1 > v2返回正数，v1 = v2返回0
     */
    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".")
        val parts2 = v2.split(".")
        val maxLength = maxOf(parts1.size, parts2.size)

        for (i in 0 until maxLength) {
            val part1 = if (i < parts1.size) parts1[i].toIntOrNull() ?: 0 else 0
            val part2 = if (i < parts2.size) parts2[i].toIntOrNull() ?: 0 else 0

            if (part1 < part2) {
                return -1
            } else if (part1 > part2) {
                return 1
            }
        }

        return 0
    }
}
