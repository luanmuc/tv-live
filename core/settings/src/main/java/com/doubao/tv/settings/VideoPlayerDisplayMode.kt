package com.doubao.tv.settings

enum class VideoPlayerDisplayMode(val value: Int, val label: String) {
    /** 原始 */
    ORIGINAL(0, "原始"),

    /** 填充 */
    FILL(1, "填充"),

    /** 裁剪 */
    CROP(2, "裁剪"),

    /** 4:3 */
    FOUR_THREE(3, "4:3"),

    /** 16:9 */
    SIXTEEN_NINE(4, "16:9"),

    /** 2.35:1 */
    WIDE(5, "2.35:1");

    companion object {
        fun fromValue(value: Int): VideoPlayerDisplayMode {
            return entries.firstOrNull { it.value == value } ?: ORIGINAL
        }
    }
}