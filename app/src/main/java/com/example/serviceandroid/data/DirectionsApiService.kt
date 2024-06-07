package com.example.serviceandroid.data

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Url

interface DirectionsApiService {
    @GET
    fun getDirections(@Url url: String): Call<DirectionsResponse>
}