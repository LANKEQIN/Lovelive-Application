package com.lovelive.dreamycolor.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.lovelive.dreamycolor.R
import org.json.JSONArray
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class BirthdayWidget : AppWidgetProvider() {
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_birthday)
            
            // 从JSON文件读取数据
            val characters = loadJsonData(context, "characters.json")
            val voiceActors = loadJsonData(context, "voice_actors.json")
            
            // 合并数据并找到最近的生日
            val allData = characters + voiceActors
            val upcomingBirthday = findUpcomingBirthday(allData)
            
            // 更新UI
            views.setTextViewText(R.id.widget_name, upcomingBirthday?.name ?: "无数据")
            views.setTextViewText(R.id.widget_birthday, upcomingBirthday?.birthday ?: "")
            views.setTextViewText(
                R.id.widget_days_left, 
                upcomingBirthday?.let { calculateDaysLeft(it.birthday) } ?: ""
            )
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
    
    private fun loadJsonData(context: Context, fileName: String): List<BirthdayData> {
        return try {
            val inputStream: InputStream = context.assets.open(fileName)
            val json = inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(json)
            
            (0 until jsonArray.length()).map { i ->
                val item = jsonArray.getJSONObject(i)
                BirthdayData(
                    name = item.getString("name"),
                    birthday = item.getString("birthday")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun findUpcomingBirthday(data: List<BirthdayData>): BirthdayData? {
        val today = Calendar.getInstance()
        val currentYear = today.get(Calendar.YEAR)
        
        return data
            .mapNotNull { item ->
                try {
                    val dateFormat = if (item.birthday.contains(".")) {
                        SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
                    } else {
                        SimpleDateFormat("MM.dd", Locale.getDefault())
                    }
                    
                    val date = dateFormat.parse(
                        if (item.birthday.contains(".")) item.birthday 
                        else "$currentYear.${item.birthday}"
                    )
                    
                    Pair(item, date)
                } catch (e: Exception) {
                    null
                }
            }
            .minByOrNull { (_, date) ->
                val daysDiff = (date.time - today.timeInMillis) / (1000 * 60 * 60 * 24)
                if (daysDiff < 0) daysDiff + 365 else daysDiff
            }?.first
    }
    
    private fun calculateDaysLeft(birthday: String): String {
        return try {
            val today = Calendar.getInstance()
            val currentYear = today.get(Calendar.YEAR)
            
            val dateFormat = if (birthday.contains(".")) {
                SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
            } else {
                SimpleDateFormat("MM.dd", Locale.getDefault())
            }
            
            val date = dateFormat.parse(
                if (birthday.contains(".")) birthday 
                else "$currentYear.${birthday}"
            )
            
            val targetDate = Calendar.getInstance()
            targetDate.time = date
            
            // 如果生日已经过去，计算下一年的生日
            if (targetDate.before(today)) {
                targetDate.add(Calendar.YEAR, 1)
            }
            
            val diffInMillis = targetDate.timeInMillis - today.timeInMillis
            val daysLeft = diffInMillis / (1000 * 60 * 60 * 24)
            
            when {
                daysLeft == 0L -> "今天生日！"
                daysLeft == 1L -> "明天生日！"
                else -> "还有${daysLeft}天"
            }
            
            val timeDiff = date.time - today.timeInMillis
            val daysDiff = timeDiff / (1000 * 60 * 60 * 24)
            
            if (daysDiff >= 0) "还剩${daysDiff + 1}天" 
            else "已过${-daysDiff - 1}天"
        } catch (e: Exception) {
            ""
        }
    }
    
    data class BirthdayData(
        val name: String,
        val birthday: String
    )
}