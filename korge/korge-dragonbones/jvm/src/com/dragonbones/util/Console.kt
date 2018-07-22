package com.dragonbones.util

object Console {
    fun _assert(bool: Boolean) {}

    fun _assert(bool: Boolean, msg: String) {}

    fun warn(msg: String) {
        System.err.println(msg)
    }
}
