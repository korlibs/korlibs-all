package com.soywiz.korau.format.com.jcraft.jorbis

internal object Util {
    fun ilog(v: Int): Int {
        var v = v
        var ret = 0
        while (v != 0) {
            ret++
            v = v ushr 1
        }
        return ret
    }

    fun ilog2(v: Int): Int {
        var v = v
        var ret = 0
        while (v > 1) {
            ret++
            v = v ushr 1
        }
        return ret
    }

    fun icount(v: Int): Int {
        var v = v
        var ret = 0
        while (v != 0) {
            ret += v and 1
            v = v ushr 1
        }
        return ret
    }
}
