package com.soywiz.korau.format.net.sourceforge.lame.mp3

/**
 * Control Parameters set by User. These parameters are here for backwards
 * compatibility with the old, non-shared lib API. Please use the
 * lame_set_variablename() functions below

 * @author Ken
 */
class LameGlobalFlags {
	var num_samples = -1
	var inNumChannels = 2
	var inSampleRate = 44100
}
