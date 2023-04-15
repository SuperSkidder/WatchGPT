package me.superskidder.watchgpt.data

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface ChatGPTApi {
        @Headers("Content-Type: application/json")
        @POST("v1/chat/completions")
        fun getCompletion(
            @Header("Authorization") apiKey: String,
            @Body request: ChatGPTRequest
        ): Call<ChatGPTResponse>

    }