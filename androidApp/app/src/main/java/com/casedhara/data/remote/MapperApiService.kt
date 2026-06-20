package com.casedhara.data.remote

import com.casedhara.data.remote.dto.BatchSearchRequestDto
import com.casedhara.data.remote.dto.MapperResponseDto
import com.casedhara.data.remote.dto.SearchRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MapperApiService {

    @GET("api/v1/mapper/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("top_k") topK: Int = 5,
        @Query("law") law: String? = null,
    ): Response<MapperResponseDto>

    @POST("api/v1/mapper/search")
    suspend fun searchPost(@Body body: SearchRequestDto): Response<MapperResponseDto>

    @POST("api/v1/mapper/batch")
    suspend fun batchSearch(@Body body: BatchSearchRequestDto): Response<List<MapperResponseDto>>

    @GET("api/v1/mapper/section/ipc/{section}")
    suspend fun ipcSection(@Path("section") section: String): Response<MapperResponseDto>

    @GET("api/v1/mapper/section/bns/{section}")
    suspend fun bnsSection(@Path("section") section: String): Response<MapperResponseDto>
}
