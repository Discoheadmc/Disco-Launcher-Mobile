package com.discocock.launcher.net

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object Http {

    const val USER_AGENT = "discolauncher/disco-launcher-mobile"

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
}
