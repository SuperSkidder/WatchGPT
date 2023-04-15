package me.superskidder.watchgpt.data

data class ChatGPTRequest(
        val model: String?,
        val messages: List<ChatCompletionMessage>
    )
    