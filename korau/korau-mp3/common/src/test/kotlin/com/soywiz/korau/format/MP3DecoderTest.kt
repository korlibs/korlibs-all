package com.soywiz.korau.format

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.vfs.ResourcesVfs
import kotlin.test.assertEquals

class MP3DecoderTest {
	val formats = AudioFormats().registerStandard().registerMp3Decoder()

	// http://mpgedit.org/mpgedit/testdata/mpegdata.html
	@kotlin.test.Test
	fun testDecodeMp3() = syncTest {
		val output = ResourcesVfs["mp31.mp3"].readAudioData(formats)
		val outputBytes = formats.encodeToByteArray(output, "out.wav")

		//output.play()
		//expected.play()

		//LocalVfs("c:/temp/mp31.mp3.wav").write(outputBytes)

		val expected = ResourcesVfs["mp31.mp3.wav"].readAudioData(formats)
		val expectedBytes = formats.encodeToByteArray(expected, "out.wav")

		assertEquals(expectedBytes.toList(), outputBytes.toList())

		//LocalVfs("c:/temp/test.mp3").readAudioStream()!!.play()
		//LocalVfs("c:/temp/test3.mp3").readAudioStream()!!.play()
		//ResourcesVfs["fl1.mp1"].readAudioStream()!!.play()
		//ResourcesVfs["fl4.mp1"].readAudioStream()!!.play()
		//ResourcesVfs["fl5.mp1"].readAudioStream()!!.play()
		//ResourcesVfs["fl10.mp2"].readAudioStream()!!.play()
		//ResourcesVfs["fl13.mp2"].readAudioStream()!!.play()
		//ResourcesVfs["fl14.mp2"].readAudioStream()!!.play()
		//ResourcesVfs["fl16.mp2"].readAudioStream()!!.play()
		//ResourcesVfs["mp31_joint_stereo_vbr.mp3"].readAudioStream()!!.play()
		//LocalVfs("c:/temp/test2.mp3").readAudioStream()!!.play()
	}
}