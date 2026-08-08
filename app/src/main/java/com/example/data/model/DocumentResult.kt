package com.example.data.model

sealed class DocumentResult<out T> {
    data class Success<T>(val data: T) : DocumentResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : DocumentResult<Nothing>()
    object Loading : DocumentResult<Nothing>()
    data class UnsupportedFormat(val format: String, val details: String) : DocumentResult<Nothing>()
}
