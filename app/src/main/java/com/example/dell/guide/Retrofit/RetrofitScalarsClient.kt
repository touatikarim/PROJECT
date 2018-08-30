package com.example.dell.guide.Retrofit

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

object RetrofitScalarsClient {
    private var retrofit : Retrofit?=null
    fun getClient(baseUrl:String): Retrofit {
        if (retrofit == null)
        {
            retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build()
        }
        return retrofit!!
    }
}