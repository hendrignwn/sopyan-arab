package com.skripsi.arab.model

data class TranslateResponse(
    var code: Int?,
    var lang: String?,
    var text: List<String>?
)