package com.soywiz.korau.format

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.vfs.ResourcesVfs
import kotlin.test.Test
import kotlin.test.assertEquals

class OGGDecoderTest {
	val formats = AudioFormats().registerStandard().registerOggVorbisDecoder()

	@Test
	fun testDecodeWav() = syncTest {
		//ResourcesVfs["ogg1.ogg"].readAudioData().play()
		//ResourcesVfs["ogg1.ogg"].readAudioStream()!!.play()
		val expected = ResourcesVfs["ogg1.ogg.wav"].readAudioData(formats)
		val output = ResourcesVfs["ogg1.ogg"].readAudioData(formats)

		val expectedBytes = formats.encodeToByteArray(expected, "out.wav")
		val outputBytes = formats.encodeToByteArray(output, "out.wav")

		//output.play()
		//expected.play()
		assertEquals(expectedBytes.toList(), outputBytes.toList())

		//LocalVfs("c:/temp/test.ogg").readAudioStream()!!.play()
	}
}