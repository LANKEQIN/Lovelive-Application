package com.lovelive.dreamycolor.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import android.util.Log // 用于调试
import android.view.View
import android.widget.RemoteViews
import com.lovelive.dreamycolor.R
import org.json.JSONArray
import java.io.InputStream
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class BirthdayWidget : AppWidgetProvider() {

    // 定义最大显示数量
    private val MAX_DISPLAY_COUNT = 4

    // 定义宽度阈值 (dp)，用于决定显示多少个item，需要根据实际效果调整
    // 这些值表示：
    // < WIDTH_THRESHOLD_2 dp: 显示 1 个
    // >= WIDTH_THRESHOLD_2 dp 且 < WIDTH_THRESHOLD_3 dp: 显示 2 个
    // >= WIDTH_THRESHOLD_3 dp 且 < WIDTH_THRESHOLD_4 dp: 显示 3 个
    // >= WIDTH_THRESHOLD_4 dp: 显示 4 个
    companion object {
        private const val WIDTH_THRESHOLD_1 = 75 // 显示1个的最小宽度
        private const val WIDTH_THRESHOLD_2 = 150 // 显示2个的最小宽度
        private const val WIDTH_THRESHOLD_3 = 220 // 显示3个的最小宽度
        private const val WIDTH_THRESHOLD_4 = 330 // 显示4个的最小宽度
        private const val TAG = "BirthdayWidget" // 日志标签
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate called")
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    // 当小组件大小改变时调用
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        Log.d(TAG, "onAppWidgetOptionsChanged called for widget ID: $appWidgetId")
        updateAppWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    // 提取公共的更新逻辑
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        Log.d(TAG, "Updating widget ID: $appWidgetId")
        // 获取小组件选项和宽度
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        // 获取当前宽度 (横向模式下通常用 MAX_WIDTH，纵向用 MIN_WIDTH，这里我们关心横向拉伸)
        // 注意：在某些启动器或首次添加时，可能获取不到，给个默认值
        val currentWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
        Log.d(TAG, "Widget ID: $appWidgetId, Current Width (dp): $currentWidthDp")

        // 根据宽度计算要显示的条目数
        val displayCount = calculateDisplayCount(currentWidthDp)
        Log.d(TAG, "Widget ID: $appWidgetId, Calculated Display Count: $displayCount")


        // 使用布局文件
        val views = RemoteViews(context.packageName, R.layout.widget_birthday)

        // 从JSON文件读取数据
        val characters = loadJsonData(context, "widget_characters.json")
        val voiceActors = loadJsonData(context, "widget_voice_actors.json")

        // 合并数据并找到最近的 N 个生日 (N = displayCount)
        val allData = characters + voiceActors
        val upcomingBirthdays = findUpcomingBirthdays(allData, displayCount)
        Log.d(TAG, "Widget ID: $appWidgetId, Found ${upcomingBirthdays.size} upcoming birthdays to display.")


        // 更新UI - 循环最多 MAX_DISPLAY_COUNT 次来设置可见性和数据
        for (i in 0 until MAX_DISPLAY_COUNT) {
            val itemContainerId = context.resources.getIdentifier("widget_item_${i + 1}", "id", context.packageName)
            val nameId = context.resources.getIdentifier("widget_name_${i + 1}", "id", context.packageName)
            val birthdayId = context.resources.getIdentifier("widget_birthday_${i + 1}", "id", context.packageName)
            val daysLeftId = context.resources.getIdentifier("widget_days_left_${i + 1}", "id", context.packageName)

            // 检查资源ID是否有效（虽然理论上应该总是有效）
            if (itemContainerId == 0 || nameId == 0 || birthdayId == 0 || daysLeftId == 0) {
                Log.e(TAG, "Resource ID not found for index ${i + 1}")
                continue // 跳过这个索引
            }

            if (i < displayCount) {
                // --- 需要显示这个条目 ---
                views.setViewVisibility(itemContainerId, View.VISIBLE) // 设置容器可见

                if (i < upcomingBirthdays.size) {
                    // 如果有足够的数据，填充内容
                    val (birthdayData, daysLeft) = upcomingBirthdays[i]
                    val daysLeftString = formatDaysLeft(daysLeft)
                    val birthdayMonthDay = formatBirthdayMonthDay(birthdayData.birthday)

                    views.setTextViewText(nameId, birthdayData.name)
                    views.setTextViewText(birthdayId, birthdayMonthDay)
                    views.setTextViewText(daysLeftId, daysLeftString)
                    Log.d(TAG, "Widget ID: $appWidgetId, Populating item ${i + 1}: ${birthdayData.name}")

                } else {
                    // 如果数据不足 (例如只找到1个生日，但需要显示2个)，清空或显示占位符
                    views.setTextViewText(nameId, "-")
                    views.setTextViewText(birthdayId, "-")
                    views.setTextViewText(daysLeftId, "-")
                    Log.d(TAG, "Widget ID: $appWidgetId, No data for item ${i + 1}, showing placeholders.")
                }
            } else {
                // --- 不需要显示这个条目 ---
                views.setViewVisibility(itemContainerId, View.GONE) // 设置容器隐藏
                Log.d(TAG, "Widget ID: $appWidgetId, Hiding item ${i + 1}")
            }
        }

        // 更新小组件实例
        try {
            appWidgetManager.updateAppWidget(appWidgetId, views)
            Log.d(TAG, "Widget ID: $appWidgetId updated successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating widget ID: $appWidgetId", e)
        }
    }

    // 根据宽度计算显示数量
    private fun calculateDisplayCount(widthDp: Int): Int {
        return when {
            widthDp == 0 -> 3 // 如果宽度未知，默认显示3个 (或根据minWidth调整)
            widthDp < WIDTH_THRESHOLD_2 -> 1
            widthDp < WIDTH_THRESHOLD_3 -> 2
            widthDp < WIDTH_THRESHOLD_4 -> 3
            else -> MAX_DISPLAY_COUNT // >= WIDTH_THRESHOLD_4 显示最多4个
        }
    }


    // --- loadJsonData, findUpcomingBirthdays, calculateNextBirthdayMillis, formatDaysLeft, formatBirthdayMonthDay, BirthdayData 保持不变 ---
    // ... (省略你原来的这些函数，它们不需要修改)
    private fun loadJsonData(context: Context, fileName: String): List<BirthdayData> {
        return try {
            val inputStream: InputStream = context.assets.open(fileName)
            val json = inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(json)
            (0 until jsonArray.length()).mapNotNull { i -> // 使用 mapNotNull 自动过滤 null
                try {
                    val item = jsonArray.getJSONObject(i)
                    val birthday = item.getString("birthday")
                    // 检查生日是否为 "未知" 或空，如果是则跳过
                    if (birthday.equals("未知", ignoreCase = true) || birthday.isBlank()) {
                        null
                    } else {
                        BirthdayData(
                            name = item.getString("name"),
                            birthday = birthday // 保留原始生日字符串用于后续处理
                        )
                    }
                } catch (e: Exception) {
                    // JSON 解析错误也跳过
                    Log.e(TAG, "Error parsing JSON object at index $i in $fileName", e)
                    null
                }
            }
        } catch (e: Exception) {
            // 文件读取错误返回空列表
            Log.e(TAG, "Error loading JSON data from $fileName", e)
            emptyList()
        }
    }
    // 修改为查找 N 个最近生日，并返回包含剩余天数的 Pair 列表
    private fun findUpcomingBirthdays(data: List<BirthdayData>, count: Int): List<Pair<BirthdayData, Long>> {
        val today = Calendar.getInstance()
        // 将时间设置为当天的开始，避免时分秒影响计算
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        val todayMillis = today.timeInMillis
        return data
            .mapNotNull { item ->
                calculateNextBirthdayMillis(item.birthday, today)?.let { nextBirthdayMillis ->
                    // 计算天数差 (使用 TimeUnit 更精确)
                    val diffMillis = nextBirthdayMillis - todayMillis
                    // 确保 diffMillis >= 0，因为 calculateNextBirthdayMillis 应该返回未来的时间
                    if (diffMillis >= 0) {
                        val daysLeft = TimeUnit.MILLISECONDS.toDays(diffMillis)
                        // 如果 diffMillis > 0 但不足一天，也算 1 天
                        if (diffMillis > 0 && daysLeft == 0L) {
                            Pair(item, 1L)
                        } else {
                            Pair(item, daysLeft) // 返回数据和剩余天数
                        }
                    } else {
                        Log.w(TAG, "Calculated next birthday is in the past? Birthday: ${item.birthday}, Diff: $diffMillis")
                        null // 不应该发生，但以防万一
                    }
                }
            }
            .sortedBy { it.second } // 按剩余天数升序排序
            .take(count) // 取前 N 个
    }
    // 辅助函数：计算下一个生日的毫秒时间戳
    private fun calculateNextBirthdayMillis(birthdayString: String, today: Calendar): Long? {
        // 优先尝试 yyyy.MM.dd 格式
        val fullDateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        // 备用 MM.dd 格式
        val monthDayFormat = SimpleDateFormat("MM.dd", Locale.getDefault())
        val currentYear = today.get(Calendar.YEAR)
        val birthdayCalendar = Calendar.getInstance()
        try {
            val date = try {
                // 尝试解析完整日期
                fullDateFormat.parse(birthdayString)
            } catch (e: ParseException) {
                // 如果失败，尝试解析 月.日
                try {
                    monthDayFormat.parse(birthdayString)
                } catch (e2: ParseException) {
                    // 两种格式都失败，返回 null
                    Log.w(TAG, "Failed to parse birthday string: $birthdayString")
                    return null
                }
            } ?: return null // 如果 date 为 null (解析失败)

            birthdayCalendar.time = date
            // 如果解析只包含月日 (年份可能不正确，比如1970)，则强制设置为当前年份
            val parsedYear = birthdayCalendar.get(Calendar.YEAR)
            if (parsedYear < 1900 || parsedYear > currentYear + 10) { // 检查年份是否合理
                birthdayCalendar.set(Calendar.YEAR, currentYear)
            }

            // 将时间设置为当天的开始
            birthdayCalendar.set(Calendar.HOUR_OF_DAY, 0)
            birthdayCalendar.set(Calendar.MINUTE, 0)
            birthdayCalendar.set(Calendar.SECOND, 0)
            birthdayCalendar.set(Calendar.MILLISECOND, 0)

            // 创建一个今年的生日日期实例用于比较
            val birthdayCalThisYear = Calendar.getInstance().apply {
                timeInMillis = birthdayCalendar.timeInMillis // 获取月日等信息
                set(Calendar.YEAR, currentYear) // 强制设置为今年
                // 再次确保时分秒为0
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // 如果今年的生日已经过去 (严格小于今天零点)，则计算明年的生日
            if (birthdayCalThisYear.before(today)) {
                birthdayCalThisYear.add(Calendar.YEAR, 1)
            }

            return birthdayCalThisYear.timeInMillis
        } catch (e: Exception) {
            // 其他未知错误
            Log.e(TAG, "Error calculating next birthday for: $birthdayString", e)
            return null
        }
    }
    // 格式化剩余天数显示的字符串
    private fun formatDaysLeft(daysLeft: Long): String {
        return when {
            daysLeft == 0L -> "今天生日！"
            daysLeft == 1L -> "明天生日！"
            daysLeft > 1L -> "${daysLeft}天"
            else -> {
                Log.w(TAG, "formatDaysLeft received unexpected value: $daysLeft")
                "" // 理论上不应该出现负数或预期外的值
            }
        }
    }
    // 格式化生日为 MM.dd 格式
    private fun formatBirthdayMonthDay(birthdayString: String): String {
        val fullDateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        val monthDayFormat = SimpleDateFormat("MM.dd", Locale.getDefault())
        return try {
            val date = try {
                fullDateFormat.parse(birthdayString)
            } catch (e: ParseException) {
                try {
                    monthDayFormat.parse(birthdayString)
                } catch (e2: ParseException) {
                    // 如果两种格式都失败，返回原始字符串
                    Log.w(TAG, "Could not format birthday to MM.dd: $birthdayString")
                    return birthdayString
                }
            }
            date?.let { monthDayFormat.format(it) } ?: birthdayString // 解析失败则返回原始字符串
        } catch (e: Exception) { //捕获更广泛的异常
            Log.e(TAG, "Error formatting birthday: $birthdayString", e)
            birthdayString // 最终失败则返回原始字符串
        }
    }
    // 定义数据类
    data class BirthdayData(
        val name: String,
        val birthday: String // 存储原始生日字符串
    )

}
