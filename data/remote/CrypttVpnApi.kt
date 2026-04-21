package com.crypttvpn.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface CrypttVpnApi {
    @POST("v1/activate")
    suspend fun activate(@Body request: ActivateRequest): ActivateResponse

    @GET("v1/subscription/status")
    suspend fun getSubscriptionStatus(): SubscriptionStatus

    @POST("v1/subscription/extend")
    suspend fun extendSubscription(@Body request: ExtendRequest): ExtendResponse

    @GET("v1/servers")
    suspend fun getServers(): ServersResponse

    @GET("v1/app/version")
    suspend fun getAppVersion(@Query("platform") platform: String = "android"): AppVersion
}
