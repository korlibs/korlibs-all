package com.soywiz.korio.serialization.json

import com.soywiz.korio.serialization.*

fun Map<*, *>.toJson(mapper: ObjectMapper) = Json.encode(this, mapper)
fun Map<*, *>.toJsonUntyped() = Json.encodeUntyped(this)