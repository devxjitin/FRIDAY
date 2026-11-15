package com.nandivex.friday.network

/** A Gemini request the server refused or failed; [statusCode] is the HTTP code. */
class HttpFailure(
    val statusCode: Int,
    message: String,
    /** Server-requested wait from the Retry-After header, if it sent one. */
    val retryAfterMs: Long? = null
) : java.io.IOException(message)
