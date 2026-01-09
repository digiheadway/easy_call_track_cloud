package com.miniclick.calltrackmanage.core

/**
 * A generic sealed class for representing the result of an operation.
 * This provides a standardized way to handle success, error, and loading states
 * throughout the application.
 * 
 * Usage:
 * ```kotlin
 * when (result) {
 *     is Result.Success -> handleData(result.data)
 *     is Result.Error -> handleError(result.exception, result.message)
 *     is Result.Loading -> showLoading()
 * }
 * ```
 */
sealed class Result<out T> {
    
    /**
     * Represents a successful operation with data.
     */
    data class Success<T>(val data: T) : Result<T>()
    
    /**
     * Represents a failed operation with optional exception and message.
     */
    data class Error(
        val exception: Throwable? = null,
        val message: String? = exception?.localizedMessage ?: "Unknown error occurred"
    ) : Result<Nothing>()
    
    /**
     * Represents an operation in progress.
     */
    data object Loading : Result<Nothing>()
    
    /**
     * Returns true if this is a Success result.
     */
    val isSuccess: Boolean get() = this is Success
    
    /**
     * Returns true if this is an Error result.
     */
    val isError: Boolean get() = this is Error
    
    /**
     * Returns true if this is a Loading result.
     */
    val isLoading: Boolean get() = this is Loading
    
    /**
     * Returns the data if this is a Success, null otherwise.
     */
    fun getOrNull(): T? = (this as? Success)?.data
    
    /**
     * Returns the data if this is a Success, or the default value otherwise.
     */
    fun getOrDefault(default: @UnsafeVariance T): T = getOrNull() ?: default
    
    /**
     * Returns the exception if this is an Error, null otherwise.
     */
    fun exceptionOrNull(): Throwable? = (this as? Error)?.exception
    
    /**
     * Maps the success value to another type.
     */
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> this
    }
    
    /**
     * Performs an action if this is a Success.
     */
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }
    
    /**
     * Performs an action if this is an Error.
     */
    inline fun onError(action: (Throwable?, String?) -> Unit): Result<T> {
        if (this is Error) action(exception, message)
        return this
    }
    
    /**
     * Performs an action if this is Loading.
     */
    inline fun onLoading(action: () -> Unit): Result<T> {
        if (this is Loading) action()
        return this
    }
    
    companion object {
        /**
         * Wraps a suspending block in a Result, catching any exceptions.
         */
        suspend inline fun <T> runCatching(block: suspend () -> T): Result<T> {
            return try {
                Success(block())
            } catch (e: Exception) {
                Error(e)
            }
        }
        
        /**
         * Wraps a block in a Result, catching any exceptions.
         */
        inline fun <T> catching(block: () -> T): Result<T> {
            return try {
                Success(block())
            } catch (e: Exception) {
                Error(e)
            }
        }
    }
}

/**
 * Converts a nullable value to a Result.
 */
fun <T> T?.toResult(errorMessage: String = "Value is null"): Result<T> {
    return if (this != null) Result.Success(this) else Result.Error(message = errorMessage)
}
