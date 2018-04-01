package com.soywiz.korau.format.net.sourceforge.lame.mp3

class MP3Data {
	var header_parsed: Boolean = false
	var stereo: Int = 0
	var samplerate: Int = 0
	var bitrate: Int = 0
	var mode: Int = 0
	var mode_ext: Int = 0
	var frameSize: Int = 0
	var numSamples: Int = 0
	var totalFrames: Int = 0
	var framesDecodedCounter: Int = 0
}
