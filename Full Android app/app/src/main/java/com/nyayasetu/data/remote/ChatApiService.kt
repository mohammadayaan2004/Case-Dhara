package com.nyayasetu.data.remote

import com.nyayasetu.data.remote.dto.ChatRequestDto
import com.nyayasetu.data.remote.dto.ChatResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ChatApiService {

    @POST("api/v1/chat/message")
    suspend fun sendMessage(@Body body: ChatRequestDto): Response<ChatResponseDto>
}
