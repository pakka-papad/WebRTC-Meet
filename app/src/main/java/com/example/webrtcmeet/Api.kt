package com.example.webrtcmeet

import com.example.webrtcmeet.Constants.API_BASE_URL
import com.example.webrtcmeet.models.ResObj
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface Api {
    @GET("api/")
    suspend fun getRandomUsers(): Response<ResObj>

}