package com.doubao.tv.core.data.repositories.git.parser

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.doubao.tv.core.data.entities.git.GitRelease
import com.doubao.tv.core.data.utils.Constants
import com.doubao.tv.core.data.utils.Globals

/**
 * 自定义发行版解析
 */
class CustomGitReleaseParser : GitReleaseParser {
    override fun isSupport(url: String): Boolean {
        return url.contains("raw.githubusercontent.com")
    }

    override suspend fun parse(data: String): GitRelease {
        val json = Globals.json.parseToJsonElement(data).jsonObject

        return GitRelease(
            version = json.getValue("tag_name").jsonPrimitive.content.substring(1),
            downloadUrl = Constants.GITHUB_PROXY + json.getValue("assets").jsonArray[0].jsonObject["browser_download_url"]!!.jsonPrimitive.content,
            description = json.getValue("body").jsonPrimitive.content,
        )
    }
}