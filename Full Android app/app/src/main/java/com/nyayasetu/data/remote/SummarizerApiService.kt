package com.nyayasetu.data.remote

import com.nyayasetu.data.remote.dto.CaseSummaryDto
import com.nyayasetu.data.remote.dto.SummarizeTextRequestDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface SummarizerApiService {

    @Multipart
    @POST("api/v1/summarize/pdf")
    suspend fun summarizePdf(@Part file: MultipartBody.Part): Response<CaseSummaryDto>

    @POST("api/v1/summarize/text")
    suspend fun summarizeText(@Body body: SummarizeTextRequestDto): Response<CaseSummaryDto>
}
