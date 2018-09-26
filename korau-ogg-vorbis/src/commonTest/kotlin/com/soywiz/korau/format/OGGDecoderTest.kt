package com.soywiz.korau.format

import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.util.*
import kotlin.test.*

class OGGDecoderTest {
	val formats = AudioFormats().registerStandard().registerOggVorbisDecoder()

	@Test
	fun testDecodeWav() = suspendTest {
		//ResourcesVfs["ogg1.ogg"].readAudioData().play()
		//ResourcesVfs["ogg1.ogg"].readAudioStream()!!.play()
		val expected = ResourcesVfs["ogg1.ogg.wav"].readAudioData(formats)
		val output = ResourcesVfs["ogg1.ogg"].readAudioData(formats)

		val expectedBytes = formats.encodeToByteArray(expected, "out.wav")
		val outputBytes = formats.encodeToByteArray(output, "out.wav")

		//output.play()
		//expected.play()
		//assertEquals(expectedBytes.toList(), outputBytes.toList())

		//LocalVfs("c:/temp/test.ogg").readAudioStream()!!.play()

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