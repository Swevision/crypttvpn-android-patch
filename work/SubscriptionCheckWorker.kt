package com.crypttvpn.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.crypttvpn.data.remote.CrypttVpnApi
import com.crypttvpn.data.storage.SecureStorage
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import retrofit2.HttpException

@HiltWorker
class SubscriptionCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val api: CrypttVpnApi,
    private val storage: SecureStorage,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (storage.token.isNullOrEmpty()) return Result.success()
        return try {
            val status = api.getSubscriptionStatus()
            if (status.status != "active") {
                // TODO: stop VPN service, post user notification
            }
            Result.success()
        } catch (e: HttpException) {
            if (e.code() == 401) {
                storage.clearAuth()
                // TODO: post "subscription expired/revoked" notification
                Result.success()
            } else Result.retry()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
