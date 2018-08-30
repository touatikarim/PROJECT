package com.example.dell.guide.URL

import com.example.dell.guide.Model.Results
import com.example.dell.guide.Retrofit.IGoogleAPIService
import com.example.dell.guide.Retrofit.RetrofitClient
import com.example.dell.guide.Retrofit.RetrofitScalarsClient

object Common {
    private val GOOGLE_API_URL="https://maps.googleapis.com/"
    var currentResult:Results?=null
    val googleApiService:IGoogleAPIService
    get()= RetrofitClient.getClient(GOOGLE_API_URL).create(IGoogleAPIService::class.java)

    val googleApiServiceScalars:IGoogleAPIService
        get()= RetrofitScalarsClient.getClient(GOOGLE_API_URL).create(IGoogleAPIService::class.java)
}