package com.soywiz.korag

import com.soywiz.korag.software.AGFactorySoftware
import com.soywiz.korio.async.syncTest
import org.junit.Test

class AGTest {
	@Test
	fun testOnReady() = syncTest {
		val ag = AGFactorySoftware().create()
		val buffer = ag.createIndexBuffer()
		buffer.upload(intArrayOf(1, 2, 3, 4))
		ag.onReady.await()
	}
}