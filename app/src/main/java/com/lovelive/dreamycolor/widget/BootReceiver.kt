package com.lovelive.dreamycolor.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    private val TAG = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device boot completed, rescheduling widget updates.")
            // 调用 BirthdayWidget 中的静态或伴生对象方法来重新安排闹钟
            // 注意：这里假设 scheduleNextUpdate 是 BirthdayWidget 伴生对象中的一个公共方法
            // 如果不是，你需要调整 BirthdayWidget 的代码或通过其他方式触发更新调度
            try {
                // 直接调用伴生对象的方法
                BirthdayWidget.scheduleNextUpdate(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling widget update on boot", e)
            }
        }
    }
}