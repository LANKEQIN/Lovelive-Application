package com.lovelive.dreamycolor.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

fun Context.copyToClipboard(text: String, label: String = "复制的文本") {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(this, "已复制：$text", Toast.LENGTH_SHORT).show()
}
