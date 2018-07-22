package com.dragonbones.util

import java.util.Objects

// @TODO: Implement using reflection getter methods, or fields
object Dynamic {

    @JvmOverloads
    operator fun get(rawData: Any, key: String, defaultValue: Any? = null): Any? {
        var out = defaultValue
        try {
            if (rawData is Map<*, *>) {
                out = rawData[key]
                if (out != null) return out
            } else if (rawData is List<*>) {
                out = rawData[Integer.parseInt(key)]
                if (out != null) return out
            } else if (rawData is ArrayBase<*>) {
                out = rawData.getObject(Integer.parseInt(key))
                if (out != null) return out
            }
        } catch (e: Throwable) {
        }

        return out
    }

    fun `in`(rawData: Any, key: String): Boolean {
        return get(rawData, key, null) != null
    }

    fun getIntArray(rawData: Any, key: String): IntArray? {
        val obj = get(rawData, key)
        if (obj is IntArray) {
            return obj
        } else if (obj is Iterable<*>) {
            val out = IntArray()
            for (o in (obj as Iterable<Any>?)!!) out.push(castInt(o, 0))
            return out
        } else {
            return null
        }
    }

    fun getFloatArray(rawData: Any, key: String): FloatArray? {
        val obj = get(rawData, key)
        if (obj is FloatArray) {
            return obj
        } else if (obj is Iterable<*>) {
            val out = FloatArray()
            for (o in (obj as Iterable<Any>?)!!) out.push(castFloat(o, 0f))
            return out
        } else {
            return null
        }
    }

    fun <T> getArray(rawData: Any, key: String): Array<T>? {
        return castArray(get(rawData, key))
    }

    @JvmOverloads
    fun getBool(rawData: Any, key: String, defaultValue: Boolean = false): Boolean {
        return castBool(get(rawData, key), defaultValue)
    }

    @JvmOverloads
    fun getInt(rawData: Any, key: String, defaultValue: Int = 0): Int {
        return getDouble(rawData, key, defaultValue.toDouble()).toInt()
    }

    @JvmOverloads
    fun getFloat(rawData: Any, key: String, defaultValue: Float = 0f): Float {
        return getDouble(rawData, key, defaultValue.toDouble()).toFloat()
    }

    @JvmOverloads
    fun getDouble(rawData: Any, key: String, defaultValue: Double = 0.0): Double {
        return castDouble(get(rawData, key), defaultValue)
    }

    @JvmOverloads
    fun getString(rawData: Any, key: String, defaultValue: String? = null): String? {
        val out = get(rawData, key) ?: return defaultValue
        return Objects.toString(out)
    }

    fun <T> castArray(obj: Any?): Array<T>? {
        try {
            if (obj is Array<*>) {
                return obj as Array<T>?
            } else if (obj is Iterable<*>) {
                val out = Array<T>()
                for (o in (obj as Iterable<T>?)!!) out.push(o)
                return out
            } else {
                return null
            }
        } catch (e: Throwable) {
            return null
        }

    }

    fun castInt(obj: Any, defaultValue: Int): Int {
        return castDouble(obj, defaultValue.toDouble()).toInt()
    }

    fun castFloat(obj: Any, defaultValue: Float): Float {
        return castDouble(obj, defaultValue.toDouble()).toFloat()
    }

    fun castBool(obj: Any?, defaultValue: Boolean): Boolean {
        if (obj == "true") return true
        return if (obj == "false") false else castDouble(obj, (if (defaultValue) 1 else 0).toDouble()).toInt() != 0
    }

    fun castDouble(obj: Any?, defaultValue: Double): Double {
        try {
            if (obj == null) return defaultValue
            if (obj is Number) {
                return obj.toDouble()
            }
            return if (obj is Boolean) {
                (if (obj) 1 else 0).toDouble()
            } else java.lang.Double.parseDouble(Objects.toString(obj))
        } catch (e: Throwable) {
            return defaultValue
        }

    }
}
