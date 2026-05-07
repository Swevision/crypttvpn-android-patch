package com.crypttvpn.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppVersionDto(
    @SerialName("latest_version") val latestVersion: String,
    @SerialName("minimum_supported_version") val minimumSupportedVersion: String,
    @SerialName("apk_url") val apkUrl: String,
    @SerialName("release_notes") val releaseNotes: String = "",
    @SerialName("mandatory_update") val mandatoryUpdate: Boolean = false,
    @SerialName("sha256") val sha256: String = "",
)
