package com.example.data.remote

import android.util.Log
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write")
}

/** Diagnostic logs must not contain account identifiers, provider emails or document contents. */
fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    val sanitizedPath = path?.replace(Regex("users/[^/]+"), "users/[redacted]")
    val jsonString = JSONObject().apply {
        put("errorType", exception.javaClass.simpleName)
        put("operationType", operationType.value)
        put("path", sanitizedPath)
    }.toString()
    Log.e("FirestoreError", jsonString)
    return jsonString
}
