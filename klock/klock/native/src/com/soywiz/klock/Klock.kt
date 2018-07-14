package com.soywiz.klock

import kotlinx.cinterop.*
import platform.posix.*

actual object Klock {
	actual fun currentTimeMillis(): Long = kotlin.system.getTimeMillis()

	private val microStart = kotlin.system.getTimeMicros()

	actual fun microClock(): Double = (kotlin.system.getTimeMicros() - microStart).toDouble()

	actual fun currentTimeMillisDouble(): Double = currentTimeMillis().toDouble()

	actual fun getLocalTimezoneOffset(unix: Long): Int = memScoped {
		val t = alloc<time_tVar>()
		val tm = alloc<tm>()
		t.value = (unix / 1000L).narrow()
		localtime_r(t.ptr, tm.ptr)
		tm.tm_gmtoff.toInt() / 60
	}
}

/*
import kotlinx.cinterop.*
import kotlin.system.*
import mytest.*

actual object Klock {
    actual val VERSION: String = KlockExt.VERSION

    actual fun currentTimeMillis(): Long = memScoped {
        return getTimeMillis()
    }

    actual fun currentTimeMillisDouble(): Double {
        return currentTimeMillis().toDouble()
    }

    actual fun getLocalTimezoneOffset(unix: Long): Int = memScoped {
        return mytest.getLocalTimezoneOffsetMinutes()
    }
}
*/
