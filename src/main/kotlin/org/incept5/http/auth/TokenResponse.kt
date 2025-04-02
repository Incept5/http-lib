package org.incept5.http.auth

/**
 * Used to store the token response from the auth server
 */
data class TokenResponse(
    val access_token: String,
    val expires_in: Int? = null,
    val refresh_token: String? = null,
    val token_type: String = "Bearer")
