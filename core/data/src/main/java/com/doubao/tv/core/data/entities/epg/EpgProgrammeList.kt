package com.doubao.tv.core.data.entities.epg

import kotlinx.serialization.Serializable

/**
 * 频道节目列表
 */
@Serializable
data class EpgProgrammeList(
    val value: List<EpgProgramme> = emptyList(),
) : List<EpgProgramme> by value
