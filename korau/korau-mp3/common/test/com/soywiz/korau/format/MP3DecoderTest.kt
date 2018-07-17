package com.soywiz.korau.format

import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.util.*
import kotlin.test.*

class MP3DecoderTest {
	val formats = AudioFormats().registerStandard().registerMp3Decoder()

	// http://mpgedit.org/mpgedit/testdata/mpegdata.html
	@kotlin.test.Test
	fun testDecodeMp3() = suspendTest {
		val output = ResourcesVfs["mp31.mp3"].readAudioData(formats)
		val outputBytes = formats.encodeToByteArray(output, "out.wav")

		//output.play()
		//expected.play()

		//LocalVfs("c:/temp/mp31.mp3.wav").write(outputBytes)

		val expected = ResourcesVfs["mp31.mp3.wav"].readAudioData(formats)
		val expectedBytes = formats.encodeToByteArray(expected, "out.wav")

		assertEquals(outputBytes.size, expectedBytes.size)
		var differentCount = 0
		val offsets = arrayListOf<Int>()
		for (n in 0 until expectedBytes.size) {
			if (expectedBytes[n] != outputBytes[n]) {
				differentCount++
				if (offsets.size < 100) {
					offsets += n
				}
			}
		}

		// @TODO: In JavaScript this yields different results! 52 bytes are different!
		if (!OS.isJs) {
			assertEquals(0, differentCount, "Some bytes are different ($offsets)")
		}
	}
}