package com.projectronin.interop.publishers.model

/**
 * Response for a publish request indicating which IDs succeeded or failed to be published.
 */
data class PublishResponse(
    val successfulIds: List<String>,
    val failedIds: List<String>,
) {
    val isSuccess: Boolean = failedIds.isEmpty()
}
