package com.specialschool.schoolapp.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import com.specialschool.schoolapp.util.Result

/**
 * [execute] 메서드가 suspend 메서드인 use case
 *
 * @see [UseCase]
 */
abstract class SuspendUseCase<in P, R>(private val dispatcher: CoroutineDispatcher) {

    suspend operator fun invoke(parameters: P): Result<R> {
        return try {
            withContext(dispatcher) {
                execute(parameters).let { Result.Success(it) }
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    protected abstract suspend fun execute(parameters: P): R
}
