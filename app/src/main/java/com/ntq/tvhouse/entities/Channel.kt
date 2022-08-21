package com.ntq.tvhouse.entities

import com.squareup.moshi.Json

data class Channel(@Json(name = "name") val name:String,@Json(name = "icon") val icon:String,@Json(name = "url") val url:String)