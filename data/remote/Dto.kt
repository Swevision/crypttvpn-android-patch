package com.crypttvpn.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActivateRequest(
    val key: String,
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_info") val deviceInfo: DeviceInfo,
)

@Serializable
data class DeviceInfo(
    val platform: String = "android",
    @SerialName("os_version") val osVersion: String,
    @SerialName("app_version") val appVersion: String,
    val model: String,
)

@Serializable
data class ActivateResponse(
    val token: String,
    val subscription: SubscriptionDto,
)

@Serializable
data class SubscriptionDto(
    val id: String,
    val plan: String,
    @SerialName("activated_at") val activatedAt: String,
    @SerialName("expires_at") val expiresAt: String,
    val status: String,
)

@Serializable
data class SubscriptionStatus(
    val status: String,
    val plan: String,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("days_left") val daysLeft: Int,
)

@Serializable
data class ExtendRequest(val key: String)

@Serializable
data class ExtendResponse(
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("extended_by_days") val extendedByDays: Int,
)

@Serializable
data class ServersResponse(val servers: List<ServerDto>)

@Serializable
data class ServerDto(
    val id: String,
    val name: String,
    @SerialName("country_code") val countryCode: String,
    @SerialName("country_name") val countryName: String,
    val city: String?,
    @SerialName("load_percent") val loadPercent: Int,
    @SerialName("is_premium") val isPremium: Boolean,
    @SerialName("vless_url") val vlessUrl: String,
)

@Serializable
data class AppVersion(
    @SerialName("latest_version") val latestVersion: String,
    @SerialName("minimum_supported_version") val minimumSupportedVersion: String,
    @SerialName("apk_url") val apkUrl: String,
    @SerialName("release_notes") val releaseNotes: String,
    @SerialName("mandatory_update") val mandatoryUpdate: Boolean,
    val sha256: String,
)
