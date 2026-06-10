package com.mobilemuuzaji.app.network

import com.mobilemuuzaji.app.network.models.AuthResponse
import com.mobilemuuzaji.app.network.models.LoginRequest
import com.mobilemuuzaji.app.network.models.SignupRequest
import com.mobilemuuzaji.app.network.models.NewOrgRequest
import com.mobilemuuzaji.app.network.models.OrganizationDetailsResponse
import com.mobilemuuzaji.app.network.models.NewInventoryRequest
import com.mobilemuuzaji.app.network.models.InventoryItemResponse
import com.mobilemuuzaji.app.network.models.UpdateInventoryRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.GET

interface ApiService {

    @POST("signup")
    suspend fun signup(@Body request: SignupRequest): Response<AuthResponse>

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("new-org")
    suspend fun createOrganization(@Body request: NewOrgRequest): Response<AuthResponse>

    @GET("organization/{org_id}")
    suspend fun getOrganization(@Path("org_id") orgId: Int): Response<OrganizationDetailsResponse>

    @POST("new-product")
    suspend fun createInventoryItem(@Body request: NewInventoryRequest): Response<InventoryItemResponse>

    @PUT("update-product/{item_id}")
    suspend fun updateInventoryItem(
        @Path("item_id") itemId: Int,
        @Body request: UpdateInventoryRequest
    ): Response<InventoryItemResponse>
}