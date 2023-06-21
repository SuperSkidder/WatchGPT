package me.superskidder.watchgpt.history

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import me.superskidder.watchgpt.MainFragment
import me.superskidder.watchgpt.config.SimpleConfig
import me.superskidder.watchgpt.data.ChatCompletionMessage

var messages: List<ChatCompletionMessage> = ArrayList()
fun saveHistory(context: Context) {
    val configManager = SimpleConfig(context)
    val gson = GsonBuilder().create()
    configManager.setString("history", gson.toJson(messages))
}

fun readHistory(context: Context) {
    val configManager = SimpleConfig(context)
    val gson = GsonBuilder().create()
    val config = configManager.getString("history", gson.toJson(messages))
    messages = gson.fromJson(config, ArrayList<ChatCompletionMessage>().javaClass)
}