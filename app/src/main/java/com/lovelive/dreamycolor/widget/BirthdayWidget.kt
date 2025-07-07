package com.lovelive.dreamycolor.widget

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.lovelive.dreamycolor.R
import org.json.JSONArray
import java.io.InputStream
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.* // 确保导入了 java.util.*
import java.util.concurrent.TimeUnit

class BirthdayWidget : AppWidgetProvider() {

    private val maxDisplayCount = 4

    companion object {
        private const val WIDTH_THRESHOLD_2 = 150
        private const val WIDTH_THRESHOLD_3 = 220
        private const val WIDTH_THRESHOLD_4 = 330
        private const val TAG = "BirthdayWidget"
        private const val ACTION_AUTO_UPDATE = "com.lovelive.dreamycolor.widget.action.AUTO_UPDATE"

        // 用于判断是否使用网格布局的阈值 (dp)
        private const val GRID_MIN_HEIGHT_THRESHOLD_DP = 100
        private const val GRID_MIN_WIDTH_THRESHOLD_DP = 100
        private const val GRID_ASPECT_RATIO_THRESHOLD = 1.5

        // 安排下一次更新
        fun scheduleNextUpdate(context: Context) { // Make it public within companion object
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, BirthdayWidget::class.java).apply {
                action = ACTION_AUTO_UPDATE
            }

            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, pendingIntentFlags)

            // 设置下一次更新时间为明天凌晨 00:01
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 1) // 稍微错开整点，避免系统拥堵
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_YEAR, 1) // 设置为明天
            }

            // 使用 setInexactRepeating 允许系统优化，更省电，但时间可能不精确
            // 如果需要精确时间，考虑 setExact 或 setAlarmClock，但需处理权限和耗电
            // 对于生日提醒，每天更新一次即可，setInexactRepeating 通常足够
            try {
                // 取消之前的闹钟，以防重复设置
                alarmManager.cancel(pendingIntent)
                // 设置重复闹钟，间隔一天
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled next update for: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(calendar.time)}")
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException: Check for SCHEDULE_EXACT_ALARM or USE_EXACT_ALARM permission if needed.", e)
                // 根据 Android 版本和需求，可能需要引导用户授予权限
            }
        }

        // 辅助函数，用于手动触发更新（例如，从配置活动）
        fun manualUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, BirthdayWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, BirthdayWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
                Log.d(TAG, "Manual update triggered for widget IDs: ${appWidgetIds.joinToString()}")
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        // 响应自定义的更新 Action
        if (ACTION_AUTO_UPDATE == intent.action) {
            Log.d(TAG, "Received auto update intent")
            // 获取所有此 provider 的 widget IDs
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, BirthdayWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate called for widget IDs: ${appWidgetIds.joinToString()}")
        // 在每次更新时重新安排下一次自动更新
        scheduleNextUpdate(context)
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

    @SuppressLint("DiscouragedApi")
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        Log.d(TAG, "Updating widget ID: $appWidgetId")
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)

        val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val maxWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
        val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        val maxHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)

        val currentWidthDp = maxWidthDp
        val currentHeightDp = maxHeightDp

        Log.d(TAG, "Widget ID: $appWidgetId, Dimensions (dp): minW=$minWidthDp, maxW=$maxWidthDp, minH=$minHeightDp, maxH=$maxHeightDp")

        val useGrid = shouldUseGridLayout(currentWidthDp, currentHeightDp)
        val layoutId = if (useGrid) {
            Log.d(TAG, "Widget ID: $appWidgetId, Using GRID layout")
            R.layout.widget_birthday_grid
        } else {
            Log.d(TAG, "Widget ID: $appWidgetId, Using ROW layout")
            R.layout.widget_birthday
        }

        val displayCount = if (useGrid) {
            this.maxDisplayCount
        } else {
            calculateRowDisplayCount(currentWidthDp)
        }
        Log.d(TAG, "Widget ID: $appWidgetId, Calculated Display Count: $displayCount (Grid: $useGrid)")

        val views = RemoteViews(context.packageName, layoutId)

        val characters = loadJsonData(context, "widget_characters.json")
        val voiceActors = loadJsonData(context, "widget_voice_actors.json")
        val allData = characters + voiceActors

        val dataFetchCount = if (useGrid) maxDisplayCount else displayCount
        val upcomingBirthdays = findUpcomingBirthdays(allData, dataFetchCount) // findUpcomingBirthdays 现在内部处理 today
        Log.d(TAG, "Widget ID: $appWidgetId, Found ${upcomingBirthdays.size} upcoming birthdays to display.")

        for (i in 0 until maxDisplayCount) {
            val itemContainerId = context.resources.getIdentifier("widget_item_${i + 1}", "id", context.packageName)
            val nameId = context.resources.getIdentifier("widget_name_${i + 1}", "id", context.packageName)
            val birthdayId = context.resources.getIdentifier("widget_birthday_${i + 1}", "id", context.packageName)
            val daysLeftId = context.resources.getIdentifier("widget_days_left_${i + 1}", "id", context.packageName)

            if (itemContainerId == 0 || nameId == 0 || birthdayId == 0 || daysLeftId == 0) {
                Log.e(TAG, "Resource ID not found for index ${i + 1} in layout $layoutId")
                continue
            }

            val shouldShow = if (useGrid) {
                i < upcomingBirthdays.size
            } else {
                i < displayCount && i < upcomingBirthdays.size
            }

            if (shouldShow) {
                views.setViewVisibility(itemContainerId, View.VISIBLE)
                val (birthdayData, daysLeft) = upcomingBirthdays[i]
                val daysLeftString = formatDaysLeft(daysLeft)
                // 使用修复后的 formatBirthdayMonthDay
                val birthdayMonthDay = formatBirthdayMonthDay(birthdayData.birthday)

                views.setTextViewText(nameId, birthdayData.name)
                views.setTextViewText(birthdayId, birthdayMonthDay)
                views.setTextViewText(daysLeftId, daysLeftString)
                Log.d(TAG, "Widget ID: $appWidgetId, Populating item ${i + 1}: ${birthdayData.name}")

            } else {
                views.setViewVisibility(itemContainerId, View.GONE)
                // 清空文本可能不是必须的，因为 GONE 会隐藏它们，但为了保险可以保留
                views.setTextViewText(nameId, "-")
                views.setTextViewText(birthdayId, "-")
                views.setTextViewText(daysLeftId, "-")
                Log.d(TAG, "Widget ID: $appWidgetId, Hiding or clearing item ${i + 1}")
            }
        }

        try {
            appWidgetManager.updateAppWidget(appWidgetId, views)
            Log.d(TAG, "Widget ID: $appWidgetId updated successfully with layout ID: $layoutId.")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating widget ID: $appWidgetId", e)
        }
    }

    private fun shouldUseGridLayout(widthDp: Int, heightDp: Int): Boolean {
        if (widthDp == 0 || heightDp == 0) return false

        val aspectRatioWidthToHeight = if (heightDp > 0) widthDp.toFloat() / heightDp.toFloat() else Float.MAX_VALUE
        val aspectRatioHeightToWidth = if (widthDp > 0) heightDp.toFloat() / widthDp.toFloat() else Float.MAX_VALUE

        val isNearSquare = aspectRatioWidthToHeight < GRID_ASPECT_RATIO_THRESHOLD &&
                aspectRatioHeightToWidth < GRID_ASPECT_RATIO_THRESHOLD

        val meetsMinSize = heightDp >= GRID_MIN_HEIGHT_THRESHOLD_DP && widthDp >= GRID_MIN_WIDTH_THRESHOLD_DP

        return isNearSquare && meetsMinSize
    }

    private fun calculateRowDisplayCount(widthDp: Int): Int {
        return when {
            widthDp == 0 -> 3 // Default if width is unknown
            widthDp < WIDTH_THRESHOLD_2 -> 1
            widthDp < WIDTH_THRESHOLD_3 -> 2
            widthDp < WIDTH_THRESHOLD_4 -> 3
            else -> maxDisplayCount
        }
    }

    private fun loadJsonData(context: Context, fileName: String): List<BirthdayData> {
        return try {
            val inputStream: InputStream = context.assets.open(fileName)
            val json = inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(json)
            (0 until jsonArray.length()).mapNotNull { i ->
                try {
                    val item = jsonArray.getJSONObject(i)
                    val birthday = item.getString("birthday")
                    if (birthday.equals("未知", ignoreCase = true) || birthday.isBlank()) {
                        null
                    } else {
                        BirthdayData(
                            name = item.getString("name"),
                            birthday = birthday
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing JSON object at index $i in $fileName", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading JSON data from $fileName", e)
            emptyList()
        }
    }

    // 修改为查找 N 个最近生日，并返回包含剩余天数的 Pair 列表
    private fun findUpcomingBirthdays(data: List<BirthdayData>, count: Int): List<Pair<BirthdayData, Long>> {
        // *** 在这里准备 today 对象 ***
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // 不再需要 todayMillis，直接传递 today 对象

        return data
            .mapNotNull { item ->
                // *** 调用修复后的 calculateNextBirthdayMillis，并传递 today ***
                calculateNextBirthdayMillis(item.birthday, today)?.let { nextBirthdayMillis ->
                    // 计算天数差 (使用 TimeUnit 更精确)
                    // 注意：这里比较的是 nextBirthdayMillis 和 today.timeInMillis
                    val diffMillis = nextBirthdayMillis - today.timeInMillis
                    if (diffMillis >= 0) {
                        var daysLeft = TimeUnit.MILLISECONDS.toDays(diffMillis)
                        // 处理当天生日的情况：如果时间差小于一天但大于0，算作0天（因为已经是今天）
                        // 如果时间差正好是0（同一天的0点），也算0天
                        // 但为了显示 "今天生日"，我们可以在 formatDaysLeft 中处理 daysLeft == 0 的情况
                        // 这里我们处理一个特殊情况：如果 diffMillis > 0 但不足一天（即在今天晚些时候），我们应该算 0 天
                        if (diffMillis > 0 && daysLeft == 0L) {
                            // 这种情况实际是今天生日，所以 daysLeft 应该是 0
                            daysLeft = 0L // 明确设置为0
                        }
                        // 返回数据和剩余天数
                        Pair(item, daysLeft)
                    } else {
                        // diffMillis < 0 意味着计算出的下一个生日在 today 之前，这不应该发生
                        Log.w(TAG, "Calculated next birthday is in the past? Birthday: ${item.birthday}, DiffMillis: $diffMillis, Today: ${today.timeInMillis}, Next: $nextBirthdayMillis")
                        null
                    }
                }
            }
            .sortedBy { it.second } // 按剩余天数升序排序
            .take(count) // 取前 N 个
    }

    // *** 修复后的 calculateNextBirthdayMillis ***
    private fun calculateNextBirthdayMillis(birthdayString: String, today: Calendar): Long? {
        // 优先尝试 yyyy.MM.dd 格式
        val fullDateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        // 备用 MM.dd 格式
        val monthDayFormat = SimpleDateFormat("MM.dd", Locale.getDefault())
        val currentYear = today.get(Calendar.YEAR)
        val birthdayCalendar = Calendar.getInstance()

        try {
            val date: Date? = try {
                // 尝试 1: 解析 yyyy.MM.dd
                fullDateFormat.isLenient = false // 设置为 false，严格匹配
                fullDateFormat.parse(birthdayString)
            } catch (e1: ParseException) {
                // 尝试 1 失败，尝试 2: 解析 MM.dd
                try {
                    monthDayFormat.isLenient = false // 设置为 false
                    monthDayFormat.parse(birthdayString)
                } catch (e2: ParseException) {
                    // 两种格式都失败
                    Log.e(TAG, "Failed to parse birthday string with any known format: $birthdayString", e2)
                    return null // 彻底失败，返回 null
                }
            }

            // 如果 date 仍然是 null (理论上不太可能，但加一层保险)
            if (date == null) {
                Log.e(TAG, "Parsing resulted in null date unexpectedly for: $birthdayString")
                return null
            }

            // --- 解析成功后的逻辑 ---
            birthdayCalendar.time = date

            // 设置生日年份为今年，用于比较
            birthdayCalendar.set(Calendar.YEAR, currentYear)
            // 将时间设置为当天的开始，以便准确比较日期
            birthdayCalendar.set(Calendar.HOUR_OF_DAY, 0)
            birthdayCalendar.set(Calendar.MINUTE, 0)
            birthdayCalendar.set(Calendar.SECOND, 0)
            birthdayCalendar.set(Calendar.MILLISECOND, 0)

            // 比较 birthdayCalendar (今年的生日) 和 today (今天的开始)
            // 如果今年的生日已经过去 (严格小于今天的开始时间)
            if (birthdayCalendar.before(today)) {
                // 计算明年的生日
                birthdayCalendar.add(Calendar.YEAR, 1)
            }
            // 否则，今年的生日还没到或就是今天，就用 birthdayCalendar 当前的时间戳

            return birthdayCalendar.timeInMillis

        } catch (e: Exception) { // 捕获其他可能的异常，例如 Calendar 操作错误
            Log.e(TAG, "Unexpected error calculating next birthday for: $birthdayString", e)
            return null
        }
    }

    // 格式化剩余天数显示的字符串 (保持不变)
    private fun formatDaysLeft(daysLeft: Long): String {
        return when {
            daysLeft == 0L -> "今天生日！"
            daysLeft == 1L -> "明天生日！"
            daysLeft > 1L -> "${daysLeft}天"
            else -> {
                Log.w(TAG, "formatDaysLeft received unexpected value: $daysLeft")
                "" // 对于负数或其他意外情况返回空字符串
            }
        }
    }

    // *** 修复后的 formatBirthdayMonthDay ***
    // 格式化生日为 MM.dd 格式，增加健壮性
    private fun formatBirthdayMonthDay(birthdayString: String): String {
        val fullDateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        val monthDayFormat = SimpleDateFormat("MM.dd", Locale.getDefault())
        return try {
            val date: Date? = try {
                // 尝试解析 yyyy.MM.dd
                fullDateFormat.isLenient = false
                fullDateFormat.parse(birthdayString)
            } catch (e1: ParseException) {
                // 尝试解析 MM.dd
                try {
                    monthDayFormat.isLenient = false
                    monthDayFormat.parse(birthdayString)
                } catch (e2: ParseException) {
                    // 两种格式都失败
                    Log.w(TAG, "Could not parse birthday string for formatting: $birthdayString")
                    return birthdayString // 返回原始字符串
                }
            }

            // 如果 date 不为 null，则格式化
            date?.let { monthDayFormat.format(it) } ?: birthdayString // 如果 date 为 null (理论上不会到这里)，返回原始字符串

        } catch (e: Exception) { // 捕获其他可能的异常
            Log.e(TAG, "Error formatting birthday: $birthdayString", e)
            birthdayString // 最终失败则返回原始字符串
        }
    }

    // 定义数据类 (保持不变)
    data class BirthdayData(
        val name: String,
        val birthday: String // 存储原始生日字符串
    )

    // 当最后一个小组件被删除时，取消闹钟
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, BirthdayWidget::class.java).apply {
            action = ACTION_AUTO_UPDATE
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE
        }
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, pendingIntentFlags)

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Cancelled scheduled updates as the last widget was disabled.")
        }
    }

    // 当第一个小组件被添加时，立即安排更新
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d(TAG, "First widget enabled, scheduling initial update.")
        scheduleNextUpdate(context)
       }
    }

