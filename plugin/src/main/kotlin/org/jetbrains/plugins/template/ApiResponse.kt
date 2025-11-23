package org.jetbrains.plugins.template

import kotlinx.serialization.Serializable

class ApiResponse {
    @Serializable
    data class ApiResponse(
        val code: String,
        val explanation: String,
        val shortDescription: String
    )
}