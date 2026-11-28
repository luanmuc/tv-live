package com.doubao.tv.core.data.utils

import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

class SourceManifestTest {
    private val manifestJson = """
        {"version":1,"files":{"pack.m3u":{"size":3,"sha256":"ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"}}}
    """.trimIndent()

    @Test
    fun `parse and hash match`() {
        val manifest = SourceManifestVerifier.parse(manifestJson.toByteArray())
        assertNotNull(manifest)
        assertTrue(
            SourceManifestVerifier.matches(
                manifest!!,
                "pack.m3u",
                "abc".toByteArray(),
            )
        )
        assertFalse(
            SourceManifestVerifier.matches(
                manifest,
                "pack.m3u",
                "abcd".toByteArray(),
            )
        )
    }

    @Test
    fun `rsa signature verifies and rejects tampering`() {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val bytes = manifestJson.toByteArray()
        val signature = Signature.getInstance("SHA256withRSA").run {
            initSign(keyPair.private)
            update(bytes)
            sign()
        }
        val encoded = Base64.getEncoder().encodeToString(signature)

        assertTrue(SourceManifestVerifier.verifySignature(bytes, encoded, keyPair.public))
        assertFalse(SourceManifestVerifier.verifySignature(bytes + " ".toByteArray(), encoded, keyPair.public))
    }

    @Test
    fun `invalid json is rejected`() {
        assertTrue(SourceManifestVerifier.parse("not json".toByteArray()) == null)
        assertTrue(SourceManifestVerifier.parse("""{"version":1,"files":{}}""".toByteArray()) != null)
        assertTrue(Json.decodeFromString<SourceManifest>("""{"version":1,"files":{}}""").files.isEmpty())
    }
}
