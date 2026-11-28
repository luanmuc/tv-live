package com.doubao.tv.core.data.repositories.iptv.parser

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class M3uIptvParserTest {
    @Test
    fun dropsUnsupportedSchemes() = runBlocking {
        val data = """
            #EXTM3U
            #EXTINF:-1,本地文件
            file:///data/secret.txt
            #EXTINF:-1,正常频道
            http://example.com/live.m3u8
        """.trimIndent()

        val channels = M3uIptvParser().parse(data)

        assertEquals(1, channels.size)
        assertEquals("http://example.com/live.m3u8", channels.single().url)
    }
}
