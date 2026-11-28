package com.doubao.tv.core.data.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamUrlTest {
    @Test
    fun acceptsMediaSchemes() {
        assertTrue(StreamUrl.isSupported("http://example.com/live.m3u8"))
        assertTrue(StreamUrl.isSupported("https://example.com/live.m3u8"))
        assertTrue(StreamUrl.isSupported("rtsp://example.com/live"))
        assertTrue(StreamUrl.isSupported("rtmp://example.com/live"))
        assertTrue(StreamUrl.isSupported("rtp://239.0.0.1:1234"))
        assertTrue(StreamUrl.isSupported("udp://239.0.0.1:1234"))
    }

    @Test
    fun rejectsNonMediaSchemes() {
        assertFalse(StreamUrl.isSupported("file:///sdcard/secret.txt"))
        assertFalse(StreamUrl.isSupported("javascript:alert(1)"))
        assertFalse(StreamUrl.isSupported("content://com.example/secret"))
        assertFalse(StreamUrl.isSupported("webview://https://example.com"))
        assertFalse(StreamUrl.isSupported("http://"))
        assertFalse(StreamUrl.isSupported(""))
    }
}
