package com.samsung.android.health.sdk.sample.healthdiary.update.data.model

import com.google.gson.annotations.SerializedName

data class UpdateResponse(
    @SerializedName("mobile") val mobile: VersionInfo,
    @SerializedName("watch") val watch: VersionInfo
)

data class VersionInfo(
    @SerializedName("version") val version: String,
    @SerializedName("url") val url: String,
    @SerializedName("force") val force: Boolean = false,
    @SerializedName("sha256") val sha256: String? = null
)
