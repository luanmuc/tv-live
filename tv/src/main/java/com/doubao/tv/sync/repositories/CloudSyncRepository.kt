package com.doubao.tv.sync.repositories

import com.doubao.tv.sync.CloudSyncData

interface CloudSyncRepository {
    suspend fun push(data: CloudSyncData): Boolean
    suspend fun pull(): CloudSyncData
}