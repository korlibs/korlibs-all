package com.soywiz.korau.format

import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlin.test.*

class AudioFormatTest {
	val formats = AudioFormats().registerStandard()

	@kotlin.test.Test
	fun wav() = suspendTest {
		assertEquals(
			"Info(lengthInMicroseconds=500000, channels=1)",
			ResourcesVfs["wav1.wav"].readSoundInfo(formats).toString()
		)
		assertEquals(
			"Info(lengthInMicroseconds=500000, channels=1)",
			ResourcesVfs["wav2.wav"].readSoundInfo(formats).toString()
		)
	}

	@kotlin.test.Test
	fun ogg() = suspendTest {
		assertEquals(
			"Info(lengthInMicroseconds=500000, channels=1)",
			ResourcesVfs["ogg1.ogg"].readSoundInfo(formats).toString()
		)
	}

	@kotlin.test.Test
	fun mp3() = suspendTest {
		assertEquals(
			"Info(lengthInMicroseconds=546625, channels=1)",
			ResourcesVfs["mp31.mp3"].readSoundInfo(formats).toString()
		)
	}
}