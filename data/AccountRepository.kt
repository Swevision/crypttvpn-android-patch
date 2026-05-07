package com.crypttvpn.data

import com.crypttvpn.data.remote.AccountApi
import com.crypttvpn.data.remote.AccountDto
import com.crypttvpn.data.remote.AppStatusDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val api: AccountApi,
) {
    suspend fun fetchAccount(): Result<AccountDto> = runCatching { api.getAccount() }
    suspend fun fetchStatus(): Result<AppStatusDto> = runCatching { api.getStatus() }
}
