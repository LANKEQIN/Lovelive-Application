package com.lovelive.dreamycolor.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.lovelive.dreamycolor.R
import org.json.JSONArray
import java.io.InputStream
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs // 导入 abs 用于计算宽高差

class BirthdayWidget : AppWidgetProvider() {

    private val MAX_DISPLAY_COUNT = 4

    // 宽度阈值 (dp) - 用于行布局
    companion object {
        private const val WIDTH_THRESHOLD_1 = 75
        private const val WIDTH_THRESHOLD_2 = 150
        private const val WIDTH_THRESHOLD_3 = 220
        private const val WIDTH_THRESHOLD_4 = 330
        private const val TAG = "BirthdayWidget"

        // 新增：用于判断是否使用网格布局的阈值 (dp)
        // 当高度大于此值，并且宽度也大于某个值（或宽高比较接近）时，使用网格
        private const val GRID_MIN_HEIGHT_THRESHOLD_DP = 100 // 示例值，需要调整
        private const val GRID_MIN_WIDTH_THRESHOLD_DP = 100  // 示例值，需要调整
        // 或者使用宽高比阈值
        private const val GRID_ASPECT_RATIO_THRESHOLD = 1.5 // 示例：如果 宽度/高度 < 1.5 且 高度/宽度 < 1.5 (比较接近正方形)
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

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        Log.d(TAG, "Updating widget ID: $appWidgetId")
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)

        // --- 获取宽度和高度 ---
        // 横向用 MAX，纵向用 MIN 可能更准，但 resizeMode 允许两者都变，我们都获取
        // 注意：竖屏时 max 是高度，横屏时 max 是宽度。这里我们假设用户通常在竖屏下调整。
        // 为了更可靠，我们获取 min/max width/height
        val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val maxWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
        val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        val maxHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)

        // 使用 maxHeight 和 maxWidth 作为当前尺寸的代表（在很多启动器上它们反映了调整后的尺寸）
        val currentWidthDp = maxWidthDp
        val currentHeightDp = maxHeightDp

        Log.d(TAG, "Widget ID: $appWidgetId, Dimensions (dp): minW=$minWidthDp, maxW=$maxWidthDp, minH=$minHeightDp, maxH=$maxHeightDp")

        // --- 判断使用哪个布局 ---
        val useGrid = shouldUseGridLayout(currentWidthDp, currentHeightDp)
        val layoutId = if (useGrid) {
            Log.d(TAG, "Widget ID: $appWidgetId, Using GRID layout")
            R.layout.widget_birthday_grid
        } else {
            Log.d(TAG, "Widget ID: $appWidgetId, Using ROW layout")
            R.layout.widget_birthday
        }

        // --- 计算显示数量 ---
        // 对于网格布局，我们总是尝试显示最多4个（如果数据允许）
        // 对于行布局，我们根据宽度计算
        val displayCount = if (useGrid) {
            MAX_DISPLAY_COUNT // 网格布局设计为最多显示4个
        } else {
            calculateRowDisplayCount(currentWidthDp) // 行布局根据宽度决定
        }
        Log.d(TAG, "Widget ID: $appWidgetId, Calculated Display Count: $displayCount (Grid: $useGrid)")

        // --- 实例化 RemoteViews ---
        val views = RemoteViews(context.packageName, layoutId) // 使用选择的布局 ID

        // --- 加载和处理数据 (这部分不变) ---
        val characters = loadJsonData(context, "widget_characters.json")
        val voiceActors = loadJsonData(context, "widget_voice_actors.json")
        val allData = characters + voiceActors
        // 即使是网格布局，我们也只获取实际需要的 displayCount 个数据，或者最多 MAX_DISPLAY_COUNT
        // findUpcomingBirthdays 现在最多返回 displayCount 个
        // 如果是网格，我们希望获取最多4个数据来填充
        val dataFetchCount = if (useGrid) MAX_DISPLAY_COUNT else displayCount
        val upcomingBirthdays = findUpcomingBirthdays(allData, dataFetchCount)
        Log.d(TAG, "Widget ID: $appWidgetId, Found ${upcomingBirthdays.size} upcoming birthdays to display.")

        // --- 更新UI (循环逻辑基本不变，因为ID相同) ---
        for (i in 0 until MAX_DISPLAY_COUNT) { // 循环总是检查所有4个可能的槽位
            val itemContainerId = context.resources.getIdentifier("widget_item_${i + 1}", "id", context.packageName)
            val nameId = context.resources.getIdentifier("widget_name_${i + 1}", "id", context.packageName)
            val birthdayId = context.resources.getIdentifier("widget_birthday_${i + 1}", "id", context.packageName)
            val daysLeftId = context.resources.getIdentifier("widget_days_left_${i + 1}", "id", context.packageName)

            if (itemContainerId == 0 || nameId == 0 || birthdayId == 0 || daysLeftId == 0) {
                Log.e(TAG, "Resource ID not found for index ${i + 1} in layout $layoutId")
                continue
            }

            // 决定这个槽位是否应该显示：
            // 1. 索引 i 必须小于计算出的 displayCount (对于行布局) 或 MAX_DISPLAY_COUNT (对于网格布局，因为我们总是尝试填满)
            // 2. 并且，必须有对应的数据 (i < upcomingBirthdays.size)
            val shouldShow = if (useGrid) {
                i < upcomingBirthdays.size // 网格布局，只要有数据就显示，最多4个
            } else {
                i < displayCount && i < upcomingBirthdays.size // 行布局，受宽度和数据限制
            }


            if (shouldShow) {
                // --- 显示并填充数据 ---
                views.setViewVisibility(itemContainerId, View.VISIBLE)
                val (birthdayData, daysLeft) = upcomingBirthdays[i]
                val daysLeftString = formatDaysLeft(daysLeft)
                val birthdayMonthDay = formatBirthdayMonthDay(birthdayData.birthday)

                views.setTextViewText(nameId, birthdayData.name)
                views.setTextViewText(birthdayId, birthdayMonthDay)
                views.setTextViewText(daysLeftId, daysLeftString)
                Log.d(TAG, "Widget ID: $appWidgetId, Populating item ${i + 1}: ${birthdayData.name}")

            } else {
                // --- 隐藏这个槽位 ---
                // 不仅要隐藏，最好也清空文本，以防万一旧文本残留
                views.setViewVisibility(itemContainerId, View.GONE)
                views.setTextViewText(nameId, "-") // 清空占位
                views.setTextViewText(birthdayId, "-")
                views.setTextViewText(daysLeftId, "-")
                Log.d(TAG, "Widget ID: $appWidgetId, Hiding or clearing item ${i + 1}")
            }
        }

        // 更新小组件实例
        try {
            appWidgetManager.updateAppWidget(appWidgetId, views)
            Log.d(TAG, "Widget ID: $appWidgetId updated successfully with layout ID: $layoutId.")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating widget ID: $appWidgetId", e)
        }
    }

    // 新增：判断是否使用网格布局的函数
    private fun shouldUseGridLayout(widthDp: Int, heightDp: Int): Boolean {
        if (widthDp == 0 || heightDp == 0) {
            // 尺寸未知时，默认使用行布局
            return false
        }

        // --- 策略1：基于绝对阈值 ---
        // return heightDp >= GRID_MIN_HEIGHT_THRESHOLD_DP && widthDp >= GRID_MIN_WIDTH_THRESHOLD_DP

        // --- 策略2：基于宽高比 ---
        // 避免除以零
        val aspectRatioWidthToHeight = widthDp.toFloat() / heightDp.toFloat()
        val aspectRatioHeightToWidth = heightDp.toFloat() / widthDp.toFloat()

        // 如果宽度和高度都比较接近（例如，比值小于1.5），则认为是方形，使用网格
        val isNearSquare = aspectRatioWidthToHeight < GRID_ASPECT_RATIO_THRESHOLD &&
                aspectRatioHeightToWidth < GRID_ASPECT_RATIO_THRESHOLD

        // 同时，可能需要一个最小尺寸要求，避免在非常小的方形时也用网格
        val meetsMinSize = heightDp >= GRID_MIN_HEIGHT_THRESHOLD_DP && widthDp >= GRID_MIN_WIDTH_THRESHOLD_DP

        // return isNearSquare // 只基于比例
        return isNearSquare && meetsMinSize // 基于比例和最小尺寸

        // --- 策略3：更简单，高度优先 ---
        // 如果高度足够大，就倾向于用网格，除非宽度特别小
        // return heightDp >= GRID_MIN_HEIGHT_THRESHOLD_DP && widthDp >= WIDTH_THRESHOLD_2 // 高度够，且宽度至少能放下2个时用网格
    }


    // 重命名旧的函数以明确其用于行布局
    private fun calculateRowDisplayCount(widthDp: Int): Int {
        return when {
            widthDp == 0 -> 3 // 宽度未知默认值
            widthDp < WIDTH_THRESHOLD_2 -> 1
            widthDp < WIDTH_THRESHOLD_3 -> 2
            widthDp < WIDTH_THRESHOLD_4 -> 3
            else -> MAX_DISPLAY_COUNT
        }
    }


    // --- loadJsonData, findUpcomingBirthdays, calculateNextBirthdayMillis, formatDaysLeft, formatBirthdayMonthDay, BirthdayData 保持不变 ---
    // ... (这些函数无需修改)
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
