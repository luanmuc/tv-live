package com.doubao.tv.core.data.network

data class HttpException(
    override val message: String?,
    override val cause: Throwable?,
) : Exception()