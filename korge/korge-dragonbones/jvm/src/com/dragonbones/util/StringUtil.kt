package com.dragonbones.util

object StringUtil {
    fun fromCharCode(cc: Int): String {
        return String(intArrayOf(cc), 0, 1)
    }

    fun fromCodePoint(cp: Int): String {
        return String(intArrayOf(cp), 0, 1)
    }
}
