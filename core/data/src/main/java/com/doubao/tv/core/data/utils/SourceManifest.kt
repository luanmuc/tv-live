package com.doubao.tv.core.data.utils

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.security.PublicKey
import java.security.Signature
import java.io.ByteArrayOutputStream
import java.util.Locale

@Serializable
data class SourceManifest(
    val version: Int = 1,
    val files: Map<String, SourceManifestFile> = emptyMap(),
)

@Serializable
data class SourceManifestFile(
    val size: Long = 0,
    val sha256: String = "",
)

/**
 * 源包清单只接受 APK 同一签名证书签出的内容。
 *
 * 清单和签名都由 CI 在发布前生成；运行时先验签，再校验每个 m3u 的 SHA-256，
 * 防止镜像被替换、CDN 返回半截文件或不同镜像文件混用。
 */
object SourceManifestVerifier {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
        explicitNulls = false
    }

    fun parse(bytes: ByteArray): SourceManifest? = runCatching {
        json.decodeFromString<SourceManifest>(bytes.decodeToString())
    }.getOrNull()

    fun verifySignature(manifest: ByteArray, signatureBase64: String, publicKey: PublicKey): Boolean =
        runCatching {
            val signature = Signature.getInstance("SHA256withRSA")
            signature.initVerify(publicKey)
            signature.update(manifest)
            signature.verify(decodeBase64(signatureBase64))
        }.getOrDefault(false)

    fun matches(manifest: SourceManifest, name: String, bytes: ByteArray): Boolean {
        val expected = manifest.files[name] ?: return false
        if (expected.size != bytes.size.toLong()) return false
        return expected.sha256.equals(sha256(bytes), ignoreCase = true)
    }

    private fun decodeBase64(input: String): ByteArray {
        val output = ByteArrayOutputStream()
        var buffer = 0
        var bits = 0
        for (raw in input) {
            val value = when (raw) {
                in 'A'..'Z' -> raw - 'A'
                in 'a'..'z' -> raw - 'a' + 26
                in '0'..'9' -> raw - '0' + 52
                '+', '-' -> 62
                '/', '_' -> 63
                '=' -> break
                else -> if (raw.isWhitespace()) continue else throw IllegalArgumentException("bad base64")
            }
            buffer = (buffer shl 6) or value
            bits += 6
            if (bits >= 8) {
                bits -= 8
                output.write((buffer shr bits) and 0xff)
            }
        }
        return output.toByteArray()
    }

    fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { String.format(Locale.US, "%02x", it.toInt() and 0xff) }
    }
}
