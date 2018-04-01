package com.soywiz.korau.format.net.sourceforge.lame.mpg

import com.soywiz.korio.JvmField

class Frame {
	@JvmField
	var stereo: Int = 0
	@JvmField
	var jsbound: Int = 0
	@JvmField
	var single: Int = 0
	@JvmField
	var lsf: Int = 0
	@JvmField
	var mpeg25: Boolean = false
	@JvmField
	var lay: Int = 0
	@JvmField
	var error_protection: Boolean = false
	@JvmField
	var bitrate_index: Int = 0
	@JvmField
	var sampling_frequency: Int = 0
	@JvmField
	var padding: Int = 0
	@JvmField
	var extension: Int = 0
	@JvmField
	var mode: Int = 0
	@JvmField
	var mode_ext: Int = 0
	@JvmField
	var copyright: Int = 0
	@JvmField
	var original: Int = 0
	@JvmField
	var emphasis: Int = 0
	@JvmField
	var framesize: Int = 0
	@JvmField
	var II_sblimit: Int = 0
	@JvmField
	var alloc: Array<L2Tables.al_table2>? = null
	@JvmField
	var down_sample_sblimit: Int = 0
	@JvmField
	var down_sample: Int = 0
}