package com.lovelive.dreamycolor.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.lovelive.dreamycolor.R
import org.json.JSONArray
import java.io.InputStream
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class BirthdayWidget : AppWidgetProvider() {
    // 定义常量，方便修改要显示的数量
    private val WIDGET_DISPLAY_COUNT = 3
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            // 使用新的布局文件
            val views = RemoteViews(context.packageName, R.layout.widget_birthday)
            // 从JSON文件读取数据
            val characters = loadJsonData(context, "widget_characters.json")
            val voiceActors = loadJsonData(context, "widget_voice_actors.json")
            // 合并数据并找到最近的 N 个生日
            val allData = characters + voiceActors
            val upcomingBirthdays = findUpcomingBirthdays(allData, WIDGET_DISPLAY_COUNT)
            // 更新UI - 循环更新每个区域
            for (i in 0 until WIDGET_DISPLAY_COUNT) {
                val nameId = context.resources.getIdentifier("widget_name_${i + 1}", "id", context.packageName)
                val birthdayId = context.resources.getIdentifier("widget_birthday_${i + 1}", "id", context.packageName)
                val daysLeftId = context.resources.getIdentifier("widget_days_left_${i + 1}", "id", context.packageName)
                if (i < upcomingBirthdays.size) {
                    val (birthdayData, daysLeft) = upcomingBirthdays[i]
                    val daysLeftString = formatDaysLeft(daysLeft)
                    // 只显示 月.日
                    val birthdayMonthDay = formatBirthdayMonthDay(birthdayData.birthday)
                    views.setTextViewText(nameId, birthdayData.name)
                    views.setTextViewText(birthdayId, birthdayMonthDay)
                    views.setTextViewText(daysLeftId, daysLeftString)
                } else {
                    // 如果没有足够的数据，清空或显示占位符
                    views.setTextViewText(nameId, "-")
                    views.setTextViewText(birthdayId, "-")
                    views.setTextViewText(daysLeftId, "-")
                }
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
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
                    null
                }
            }
        } catch (e: Exception) {
            // 文件读取错误返回空列表
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
                    val daysLeft = TimeUnit.MILLISECONDS.toDays(diffMillis)
                    Pair(item, daysLeft) // 返回数据和剩余天数
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
                // 如果失败，尝试解析 月.日，并使用今年
                try {
                    monthDayFormat.parse(birthdayString)?.also {
                        val tempCal = Calendar.getInstance()
                        tempCal.time = it
                        tempCal.set(Calendar.YEAR, currentYear)
                        // 如果解析出的日期不含年份信息，手动设置当前年份
                        if (tempCal.get(Calendar.YEAR) < 1970) { // SimpleDateFormat 可能解析出奇怪的年份
                            tempCal.set(Calendar.YEAR, currentYear)
                        }
                        birthdayCalendar.time = tempCal.time
                    }
                } catch (e2: ParseException) {
                    // 两种格式都失败，返回 null
                    return null
                }
                birthdayCalendar.time // 返回设置好年份的日期
            } ?: return null // 如果 date 为 null (解析失败)
            birthdayCalendar.time = date
            // 将时间设置为当天的开始
            birthdayCalendar.set(Calendar.HOUR_OF_DAY, 0)
            birthdayCalendar.set(Calendar.MINUTE, 0)
            birthdayCalendar.set(Calendar.SECOND, 0)
            birthdayCalendar.set(Calendar.MILLISECOND, 0)
            // 如果生日在今天之前（或年份是过去的），则计算明年的生日
            // 注意：这里要用 set 好 year 的 birthdayCalendar 和 today 比较
            val birthdayCalThisYear = Calendar.getInstance().apply {
                time = birthdayCalendar.time // 获取月日
                set(Calendar.YEAR, currentYear) // 强制设置为今年
                // 重置时分秒
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (birthdayCalThisYear.before(today)) {
                // 如果今年的生日已经过去，计算明年的
                birthdayCalThisYear.add(Calendar.YEAR, 1)
            }
            return birthdayCalThisYear.timeInMillis
        } catch (e: Exception) {
            // 其他未知错误
            return null
        }
    }
    // 格式化剩余天数显示的字符串
    private fun formatDaysLeft(daysLeft: Long): String {
        return when {
            daysLeft == 0L -> "今天生日！"
            daysLeft == 1L -> "明天生日！"
            daysLeft > 1L -> "还有${daysLeft}天"
            else -> "" // 理论上不应该出现负数，因为我们计算的是未来的生日
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
                monthDayFormat.parse(birthdayString)
            }
            date?.let { monthDayFormat.format(it) } ?: birthdayString // 解析失败则返回原始字符串
        } catch (e: ParseException) {
            birthdayString // 最终失败则返回原始字符串
        }
    }
    // 定义数据类
    data class BirthdayData(
        val name: String,
        val birthday: String // 存储原始生日字符串
    )
}