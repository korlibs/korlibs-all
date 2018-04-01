package com.soywiz.kmedialayer

@JsName("Map")
internal external class JsMap {
    fun clear(): Unit
    fun get(key: String): dynamic
    fun has(key: String): Boolean
    fun set(key: String, value: dynamic): Unit
}