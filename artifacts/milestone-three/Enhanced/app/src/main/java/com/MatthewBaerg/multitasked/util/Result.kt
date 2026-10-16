package com.MatthewBaerg.multitasked.util

/**
 * Simple sealed class to represent loading/success/error states.
 * ViewModels expose these to the UI so Composables know what to show.
 */
sealed class Result<out T> {
    object Idle : Result<Nothing>()
    object Loading : Result<Nothing>()
    data class Success<T>(val data: T) : Result<T>()
    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : Result<Nothing>()
}
