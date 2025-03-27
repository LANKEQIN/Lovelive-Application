package com.lovelive.dreamycolor.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.lovelive.dreamycolor.R

class BirthdayWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // 实现小部件更新逻辑
        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_birthday)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}