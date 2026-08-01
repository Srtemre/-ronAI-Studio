package com.htmltoapk.studio.core.result

sealed interface Result<out T> {
    data class Success<T>(val value: T) : Result<T>
    data class Failure(val error: Throwable) : Result<Nothing>
    data object Loading : Result<Nothing>
}

inline fun <T> resultOf(block: () -> T): Result<T> = try {
    Result.Success(block())
} catch (t: Throwable) {
    Result.Failure(t)
}

inline fun <T, R> Result<T>.map(block: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(block(value))
    is Result.Failure -> this
    Result.Loading -> Result.Loading
}

fun Result<*>.errorOrNull(): Throwable? = (this as? Result.Failure)?.error
fun <T> Result<T>.getOrNull(): T? = (this as? Result.Success)?.value
