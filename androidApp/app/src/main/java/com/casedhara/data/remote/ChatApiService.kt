package com.casedhara.data.remote

import com.casedhara.data.remote.dto.ChatRequestDto
import com.casedhara.data.remote.dto.ChatResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ChatApiService {

    @POST("api/v1/chat/message")
    suspend fun sendMessage(@Body body: ChatRequestDto): Response<ChatResponseDto>
}
