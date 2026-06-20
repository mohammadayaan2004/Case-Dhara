package com.casedhara.data.repository

import com.casedhara.data.remote.SummarizerApiService
import com.casedhara.data.remote.dto.SummarizeTextRequestDto
import com.casedhara.data.remote.dto.toDomain
import com.casedhara.domain.model.CaseSummary
import com.casedhara.util.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SummarizerRepository @Inject constructor(
    private val api: SummarizerApiService,
) {

    fun summarizePdf(bytes: ByteArray, fileName: String): Flow<NetworkResult<CaseSummary>> =
        flow {
            emit(NetworkResult.Loading)
            try {
                val body = bytes.toRequestBody("application/pdf".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", fileName, body)
                val response = api.summarizePdf(part)
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        emit(NetworkResult.Success(body.toDomain()))
                    } ?: emit(
                        NetworkResult.Error(
                            "Response body is null",
                            response.code(),
                        ),
                    )
                } else {
                    emit(
                        NetworkResult.Error(
                            response.errorBody()?.string() ?: "Summarization failed",
                            response.code(),
                        ),
                    )
                }
            } catch (e: Exception) {
                emit(NetworkResult.Error(e.message ?: "Network error"))
            }
        }.flowOn(Dispatchers.IO)

    fun summarizeText(text: String): Flow<NetworkResult<CaseSummary>> =
        flow {
            emit(NetworkResult.Loading)
            try {
                val response = api.summarizeText(SummarizeTextRequestDto(text = text))
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        emit(NetworkResult.Success(body.toDomain()))
                    } ?: emit(
                        NetworkResult.Error(
                            "Response body is null",
                            response.code(),
                        ),
                    )
                } else {
                    emit(
                        NetworkResult.Error(
                            response.errorBody()?.string() ?: "Summarization failed",
                            response.code(),
                        ),
                    )
                }
            } catch (e: Exception) {
                emit(NetworkResult.Error(e.message ?: "Network error"))
            }
        }.flowOn(Dispatchers.IO)
}
