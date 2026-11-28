package com.doubao.tv.core.data.entities.epg

import kotlinx.serialization.Serializable

/**
 * 节目预约列表
 */
@Serializable
data class EpgProgrammeReserveList(
    val value: List<EpgProgrammeReserve> = emptyList(),
) : List<EpgProgrammeReserve> by value
