package com.casedhara.data.repository

import com.casedhara.data.remote.ChatApiService
import com.casedhara.data.remote.dto.ChatRequestDto
import com.casedhara.data.remote.dto.toDto
import com.casedhara.domain.model.ChatMessage
import com.casedhara.domain.model.ChatReply
import com.casedhara.util.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val api: ChatApiService,
) {

    fun sendMessage(
        question: String,
        history: List<ChatMessage>,
    ): Flow<NetworkResult<ChatReply>> =
        flow {
            emit(NetworkResult.Loading)
            try {
                val dto = ChatRequestDto(
                    question = question,
                    history = history.map { it.toDto() },
                )
                val response = api.sendMessage(dto)
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        emit(
                            NetworkResult.Success(
                                ChatReply(
                                    answer = body.answer,
                                    retrievedSections = body.retrievedSections,
                                    retrievalTier = body.retrievalTier,
                                ),
                            ),
                        )
                    } ?: emit(
                        NetworkResult.Error(
                            "Response body is null",
                            response.code(),
                        ),
                    )
                } else {
                    emit(
                        NetworkResult.Error(
                            response.errorBody()?.string() ?: "Chat failed",
                            response.code(),
                        ),
                    )
                }
            } catch (e: Exception) {
                emit(NetworkResult.Error(e.message ?: "Network error"))
            }
        }.flowOn(Dispatchers.IO)
}
