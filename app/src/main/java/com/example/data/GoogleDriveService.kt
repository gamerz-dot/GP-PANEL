package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

data class DriveFileMetadataRequest(
    @Json(name = "name") val name: String,
    @Json(name = "mimeType") val mimeType: String = "application/json"
)

data class DriveFileMetadataResponse(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = null
)

interface GoogleDriveApiService {
    @POST("drive/v3/files")
    suspend fun createFileMetadata(
        @Header("Authorization") authHeader: String,
        @Body metadata: DriveFileMetadataRequest
    ): Response<DriveFileMetadataResponse>

    @PATCH("upload/drive/v3/files/{fileId}")
    suspend fun uploadFileContent(
        @Header("Authorization") authHeader: String,
        @Header("Content-Type") contentType: String,
        @Path("fileId") fileId: String,
        @Query("uploadType") uploadType: String = "media",
        @Body content: RequestBody
    ): Response<ResponseBody>
}

object GoogleDriveClient {
    private const val BASE_URL = "https://www.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val apiService: GoogleDriveApiService = retrofit.create(GoogleDriveApiService::class.java)
}
