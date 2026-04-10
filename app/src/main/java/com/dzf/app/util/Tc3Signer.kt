package com.dzf.app.util

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.net.URI
import java.net.URLEncoder
import java.security.MessageDigest

object Tc3Signer {

    private const val ALGORITHM = "TC3-HMAC-SHA256"
    private const val SERVICE = "tcb"
    private const val VERSION = "1.0"
    private const val SIGNED_HEADERS = "content-type;host"
    private const val EMPTY_PAYLOAD_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"

    enum class Mode {
        CLOUDBASE_LEGACY,
        CLOUDBASE_TCB_HOST,
        TC3_STANDARD
    }

    data class SignedHeaders(
        val authorization: String,
        val timestamp: String
    )

    fun sign(
        secretId: String,
        secretKey: String,
        httpMethod: String,
        url: String,
        requestBody: String?,
        mode: Mode
    ): SignedHeaders {
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val date = getDate(timestamp.toLong())

        val uri = URI(url)
        val canonicalParts = when (mode) {
            Mode.CLOUDBASE_LEGACY -> Triple("//api.tcloudbase.com/", "", "api.tcloudbase.com")
            Mode.CLOUDBASE_TCB_HOST -> Triple("/", "", "tcb-api.tencentcloudapi.com")
            Mode.TC3_STANDARD -> Triple(
                encodeCanonicalUri(uri.path),
                encodeCanonicalQuery(uri.rawQuery),
                "tcb-api.tencentcloudapi.com"
            )
        }

        val payloadHash = if (mode == Mode.TC3_STANDARD) {
            requestBody?.let { sha256Hex(it) } ?: EMPTY_PAYLOAD_SHA256
        } else {
            EMPTY_PAYLOAD_SHA256
        }

        val canonicalRequest = buildString {
            append(httpMethod).append("\n")
            append(canonicalParts.first).append("\n")
            append(canonicalParts.second).append("\n")
            append("content-type:application/json; charset=utf-8\n")
            append("host:${canonicalParts.third}\n")
            append("\n")
            append(SIGNED_HEADERS).append("\n")
            append(payloadHash)
        }

        val hashedCanonicalRequest = sha256Hex(canonicalRequest)
        val credentialScope = "$date/$SERVICE/tc3_request"
        val stringToSign = buildString {
            append(ALGORITHM).append("\n")
            append(timestamp).append("\n")
            append(credentialScope).append("\n")
            append(hashedCanonicalRequest)
        }

        val kDate = hmacSha256("TC3$secretKey".toByteArray(Charsets.UTF_8), date)
        val kService = hmacSha256(kDate, SERVICE)
        val kSigning = hmacSha256(kService, "tc3_request")
        val signature = bytesToHex(hmacSha256(kSigning, stringToSign))

        val authorization = buildString {
            append(ALGORITHM).append(" ")
            append("Credential=").append(secretId).append("/").append(credentialScope).append(", ")
            append("SignedHeaders=").append(SIGNED_HEADERS).append(", ")
            append("Signature=").append(signature)
        }

        return SignedHeaders(
            authorization = "$VERSION $authorization",
            timestamp = timestamp
        )
    }

    private fun hmacSha256(key: ByteArray, data: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
    }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun encodeCanonicalUri(path: String?): String {
        if (path.isNullOrEmpty()) return "/"
        val segments = path.split("/")
        return segments.joinToString("/") { seg ->
            if (seg.isEmpty()) "" else rfc3986Encode(seg)
        }.let { if (it.startsWith("/")) it else "/$it" }
    }

    private fun encodeCanonicalQuery(rawQuery: String?): String {
        if (rawQuery.isNullOrEmpty()) return ""
        val pairs = rawQuery.split("&").map {
            val idx = it.indexOf('=')
            if (idx >= 0) {
                val k = it.substring(0, idx)
                val v = it.substring(idx + 1)
                rfc3986Encode(k) + "=" + rfc3986Encode(v)
            } else {
                rfc3986Encode(it) + "="
            }
        }
        return pairs.sorted().joinToString("&")
    }

    private fun rfc3986Encode(value: String): String {
        return URLEncoder.encode(value, "UTF-8")
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~")
    }

    private fun getDate(timestamp: Long): String {
        val date = java.util.Date(timestamp * 1000)
        val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        format.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return format.format(date)
    }
}
