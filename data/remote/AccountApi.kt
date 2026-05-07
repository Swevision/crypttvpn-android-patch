package com.crypttvpn.data.remote

import retrofit2.http.GET

interface AccountApi {
    /** Personal cabinet — uses device JWT (set by AuthInterceptor). */
    @GET("v1/app/account")
    suspend fun getAccount(): AccountDto

    /** Backend-controlled banner shown across the app. */
    @GET("v1/app/status")
    suspend fun getStatus(): AppStatusDto
}
