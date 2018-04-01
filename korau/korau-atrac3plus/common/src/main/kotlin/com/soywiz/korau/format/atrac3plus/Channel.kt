package com.soywiz.korau.format.atrac3plus

import com.soywiz.korau.format.atrac3plus.Atrac3plusDecoder.Companion.ATRAC3P_SUBBANDS

/** Sound channel parameters  */
class Channel(var chNum: Int) {
	var numCodedVals: Int = 0               ///< number of transmitted quant unit values
	var fillMode: Int = 0
	var splitPoint: Int = 0
	var tableType: Int = 0                  ///< table type: 0 - tone?, 1- noise?
	var quWordlen = IntArray(32)  ///< array of word lengths for each quant unit
	var quSfIdx = IntArray(32)    ///< array of scale factor indexes for each quant unit
	var quTabIdx = IntArray(32)   ///< array of code table indexes for each quant unit
	var spectrum = IntArray(2048) ///< decoded IMDCT spectrum
	var powerLevs = IntArray(5)   ///< power compensation levels

	// imdct window shape history (2 frames) for overlapping.
	val wndShapeHist: Array<BooleanArray> = Array(2) { BooleanArray(ATRAC3P_SUBBANDS) }      ///< IMDCT window shape, 0=sine/1=steep
	var wndShape: BooleanArray = wndShapeHist[0]             ///< IMDCT window shape for current frame
	var wndShapePrev: BooleanArray = wndShapeHist[1]         ///< IMDCT window shape for previous frame

	// gain control data history (2 frames) for overlapping.
	internal val gainDataHist: Array<Array<AtracGainInfo>> = Array(2) { Array(ATRAC3P_SUBBANDS) { AtracGainInfo() } }       ///< gain control data for all subbands
	internal var gainData: Array<AtracGainInfo> = gainDataHist[0]              ///< gain control data for next frame
	internal var gainDataPrev: Array<AtracGainInfo> = gainDataHist[1]          ///< gain control data for previous frame
	var numGainSubbands: Int = 0            ///< number of subbands with gain control data

	// tones data history (2 frames) for overlapping.
	internal var tonesInfoHist: Array<Array<WavesData>> = Array<Array<WavesData>>(2) { Array(ATRAC3P_SUBBANDS) { WavesData() } }
	internal var tonesInfo: Array<WavesData> = tonesInfoHist[0]
	internal var tonesInfoPrev: Array<WavesData> = tonesInfoHist[1]
}
