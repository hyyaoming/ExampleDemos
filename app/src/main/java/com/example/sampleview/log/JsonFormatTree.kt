package com.example.sampleview.log

import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser

class JsonFormatTree : Timber.DebugTree() {

    companion object {
        private const val MAX_LOG_LENGTH = 2000
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val finalTag = tag ?: "JsonFormatTree"
        val formattedMessage = try {
            if (isJson(message)) formatJson(message) else message
        } catch (e: Exception) {
            message
        }

        var start = 0
        val length = formattedMessage.length
        while (start < length) {
            val end = (start + MAX_LOG_LENGTH).coerceAtMost(length)
            val part = formattedMessage.substring(start, end)
            Log.println(priority, finalTag, part)
            start = end
        }
    }

    private fun isJson(message: String): Boolean {
        val trimmed = message.trim()
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
               (trimmed.startsWith("[") && trimmed.endsWith("]"))
    }

    private fun formatJson(json: String): String {
        val jsonElement = JsonParser.parseString(json)
        return GsonBuilder().setPrettyPrinting().create().toJson(jsonElement)
    }
}
