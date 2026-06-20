package com.casedhara.util

sealed class NetworkResult<out T> {
    data object Idle : NetworkResult<Nothing>()
    data object Loading : NetworkResult<Nothing>()
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String, val code: Int? = null) : NetworkResult<Nothing>()
}
