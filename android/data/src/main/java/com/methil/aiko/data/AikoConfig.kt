package com.methil.data

import com.methil.data.BuildConfig

object AikoConfig {
    const val BASE_URL = "https://ethancarollo--aiko-backend-llama-cpp-serve.modal.run/v1"
    const val DEFAULT_MODEL = "aiko"
    val API_KEY = BuildConfig.AIKO_API_KEY
}