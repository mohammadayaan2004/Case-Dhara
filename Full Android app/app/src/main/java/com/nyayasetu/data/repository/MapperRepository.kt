package com.nyayasetu.data.repository

import com.nyayasetu.data.local.dao.SearchHistoryDao
import com.nyayasetu.data.local.entity.SearchHistoryEntity
import com.nyayasetu.data.remote.MapperApiService
import com.nyayasetu.data.remote.dto.SearchRequestDto
import com.nyayasetu.data.remote.dto.toDomain
import com.nyayasetu.domain.model.MapperResponse
import com.nyayasetu.util.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapperRepository @Inject constructor(
    private val api: MapperApiService,
    private val searchHistoryDao: SearchHistoryDao,
) {

    fun search(query: String, topK: Int = 5, law: String? = null): Flow<NetworkResult<MapperResponse>> =
        flow {
            emit(NetworkResult.Loading)
            try {
                val response = if (query.length > 200) {
                    api.searchPost(SearchRequestDto(query = query, topK = topK, lawFilter = law))
                } else {
                    api.search(query = query, topK = topK, law = law)
                }
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        val domain = body.toDomain()
                        val existing = searchHistoryDao.findByQuery(query)
                        if (existing != null) {
                            searchHistoryDao.update(existing.copy(timestamp = System.currentTimeMillis()))
                        } else {
                            searchHistoryDao.insert(
                                SearchHistoryEntity(query = query, timestamp = System.currentTimeMillis()),
                            )
                        }
                        emit(NetworkResult.Success(domain))
                    } ?: emit(
                        NetworkResult.Error(
                            "Response body is null",
                            response.code(),
                        ),
                    )
                } else {
                    emit(
                        NetworkResult.Error(
                            message = response.errorBody()?.string() ?: "Search failed",
                            code = response.code(),
                        ),
                    )
                }
            } catch (e: Exception) {
                emit(NetworkResult.Error(e.message ?: "Network error"))
            }
        }.flowOn(Dispatchers.IO)
}
