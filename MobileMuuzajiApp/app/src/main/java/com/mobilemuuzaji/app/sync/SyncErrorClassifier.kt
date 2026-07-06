package com.mobilemuuzaji.app.sync

data class SyncRetryDecision(
    val shouldRetry: Boolean,
    val shouldDrop: Boolean
)

object SyncErrorClassifier {

    fun classify(statusCode: Int, errorBody: String): SyncRetryDecision {
        val body = errorBody.orEmpty()

        return when {
            statusCode in 500..599 -> SyncRetryDecision(shouldRetry = true, shouldDrop = false)
            statusCode == 404 -> SyncRetryDecision(shouldRetry = false, shouldDrop = true)
            statusCode == 400 && body.contains("Not enough stock", ignoreCase = true) ->
                SyncRetryDecision(shouldRetry = false, shouldDrop = true)
            statusCode == 409 -> SyncRetryDecision(shouldRetry = false, shouldDrop = true)
            else -> SyncRetryDecision(shouldRetry = true, shouldDrop = false)
        }
    }
}
