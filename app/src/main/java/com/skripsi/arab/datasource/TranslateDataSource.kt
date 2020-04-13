package com.skripsi.arab.datasource

import com.skripsi.arab.BuildConfig
import com.skripsi.arab.model.TranslateResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface TranslateDataSource {
    @GET("v1.5/tr.json/translate")
    fun translate(
        @Query("text") text: String,
        @Query("key") key: String = BuildConfig.TRANSLATE_API_KEY,
        @Query("lang") lang: String = "id"
    ) : Call<TranslateResponse>
}