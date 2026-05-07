package com.crypttvpn.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response of GET /v1/app/account — in-app cabinet.
 * Authenticated by the device JWT (issued during activation).
 */
@Serializable
data class AccountDto(
    @SerialName("device_id") val deviceId: String,
    val subscription: AccountSubscriptionDto,
    val key: AccountKeyDto,
    val user: AccountUserDto? = null,
    @SerialName("all_keys") val allKeys: List<AccountKeyEntryDto> = emptyList(),
    @SerialName("all_subscriptions") val allSubscriptions: List<AccountSubEntryDto> = emptyList(),
    val payments: List<AccountPaymentDto> = emptyList(),
    @SerialName("audit_log") val auditLog: List<AccountAuditDto> = emptyList(),
)

@Serializable
data class AccountSubscriptionDto(
    val id: String,
    val plan: String,
    val status: String,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("days_left") val daysLeft: Int,
    @SerialName("activated_at") val activatedAt: String,
    @SerialName("expires_at") val expiresAt: String,
    val device: AccountDeviceDto? = null,
)

@Serializable
data class AccountDeviceDto(
    val name: String,
    val os: String? = null,
)

@Serializable
data class AccountKeyDto(
    val value: String,
    val status: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class AccountUserDto(
    val id: String,
    val email: String? = null,
    @SerialName("telegram_username") val telegramUsername: String? = null,
    @SerialName("display_name") val displayName: String? = null,
)

@Serializable
data class AccountKeyEntryDto(
    val key: String,
    val plan: String,
    val status: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("activated_at") val activatedAt: String? = null,
)

@Serializable
data class AccountSubEntryDto(
    val id: String,
    val plan: String,
    val status: String,
    @SerialName("activated_at") val activatedAt: String,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("device_id_last4") val deviceIdLast4: String? = null,
    val device: AccountDeviceDto? = null,
)

@Serializable
data class AccountPaymentDto(
    val id: String,
    val plan: String,
    @SerialName("amount_rub") val amountRub: String,
    val status: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("paid_at") val paidAt: String? = null,
    @SerialName("activation_key") val activationKey: String? = null,
)

@Serializable
data class AccountAuditDto(
    val id: String,
    val action: String,
    val label: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("subject_type") val subjectType: String? = null,
    @SerialName("subject_id") val subjectId: String? = null,
)

/** GET /v1/app/status — backend status banner. */
@Serializable
data class AppStatusDto(
    val active: Boolean,
    val severity: String? = null,
    val title: String? = null,
    val message: String? = null,
    @SerialName("action_url") val actionUrl: String? = null,
)
