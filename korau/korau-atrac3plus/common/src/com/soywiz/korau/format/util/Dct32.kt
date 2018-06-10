package com.soywiz.korau.format.util

// Template for the Discrete Cosine Transform for 32 samples
object Dct32 {
	/* tab[i][j] = 1.0 / (2.0 * cos(pi*(2*k+1) / 2^(6 - j))) */

	/* cos(i*pi/64) */

	val COS0_0 = (0.50060299823519630134 / 2).toFloat()
	val COS0_1 = (0.50547095989754365998 / 2).toFloat()
	val COS0_2 = (0.51544730992262454697 / 2).toFloat()
	val COS0_3 = (0.53104259108978417447 / 2).toFloat()
	val COS0_4 = (0.55310389603444452782 / 2).toFloat()
	val COS0_5 = (0.58293496820613387367 / 2).toFloat()
	val COS0_6 = (0.62250412303566481615 / 2).toFloat()
	val COS0_7 = (0.67480834145500574602 / 2).toFloat()
	val COS0_8 = (0.74453627100229844977 / 2).toFloat()
	val COS0_9 = (0.83934964541552703873 / 2).toFloat()
	val COS0_10 = (0.97256823786196069369 / 2).toFloat()
	val COS0_11 = (1.16943993343288495515 / 4).toFloat()
	val COS0_12 = (1.48416461631416627724 / 4).toFloat()
	val COS0_13 = (2.05778100995341155085 / 8).toFloat()
	val COS0_14 = (3.40760841846871878570 / 8).toFloat()
	val COS0_15 = (10.19000812354805681150 / 32).toFloat()

	val COS1_0 = (0.50241928618815570551 / 2).toFloat()
	val COS1_1 = (0.52249861493968888062 / 2).toFloat()
	val COS1_2 = (0.56694403481635770368 / 2).toFloat()
	val COS1_3 = (0.64682178335999012954 / 2).toFloat()
	val COS1_4 = (0.78815462345125022473 / 2).toFloat()
	val COS1_5 = (1.06067768599034747134 / 4).toFloat()
	val COS1_6 = (1.72244709823833392782 / 4).toFloat()
	val COS1_7 = (5.10114861868916385802 / 16).toFloat()

	val COS2_0 = (0.50979557910415916894 / 2).toFloat()
	val COS2_1 = (0.60134488693504528054 / 2).toFloat()
	val COS2_2 = (0.89997622313641570463 / 2).toFloat()
	val COS2_3 = (2.56291544774150617881 / 8).toFloat()

	val COS3_0 = (0.54119610014619698439 / 2).toFloat()
	val COS3_1 = (1.30656296487637652785 / 4).toFloat()

	val COS4_0 = (0.70710678118654752439 / 2).toFloat()

	// butterfly operator
	private fun BF(`val`: FloatArray, a: Int, b: Int, c: Float, s: Int) {
		val tmp0 = `val`[a] + `val`[b]
		val tmp1 = `val`[a] - `val`[b]
		`val`[a] = tmp0
		`val`[b] = tmp1 * c * (1 shl s).toFloat()
	}

	private fun BF0(tab: FloatArray, tabOffset: Int, `val`: FloatArray, a: Int, b: Int, c: Float, s: Int) {
		val tmp0 = tab[tabOffset + a] + tab[tabOffset + b]
		val tmp1 = tab[tabOffset + a] - tab[tabOffset + b]
		`val`[a] = tmp0
		`val`[b] = tmp1 * c * (1 shl s).toFloat()
	}

	private fun BF1(`val`: FloatArray, a: Int, b: Int, c: Int, d: Int) {
		BF(`val`, a, b, COS4_0, 1)
		BF(`val`, c, d, -COS4_0, 1)
		`val`[c] += `val`[d]
	}

	private fun BF2(`val`: FloatArray, a: Int, b: Int, c: Int, d: Int) {
		BF(`val`, a, b, COS4_0, 1)
		BF(`val`, c, d, -COS4_0, 1)
		`val`[c] += `val`[d]
		`val`[a] += `val`[c]
		`val`[c] += `val`[b]
		`val`[b] += `val`[d]
	}

	private fun ADD(`val`: FloatArray, a: Int, b: Int) {
		`val`[a] += `val`[b]
	}

	// DCT32 without 1/sqrt(2) coef zero scaling.
	fun dct32(out: FloatArray, outOffset: Int, tab: FloatArray, tabOffset: Int) {
		val `val` = FloatArray(32)

		/* pass 1 */
		BF0(tab, tabOffset, `val`, 0, 31, COS0_0, 1)
		BF0(tab, tabOffset, `val`, 15, 16, COS0_15, 5)
		/* pass 2 */
		BF(`val`, 0, 15, COS1_0, 1)
		BF(`val`, 16, 31, -COS1_0, 1)
		/* pass 1 */
		BF0(tab, tabOffset, `val`, 7, 24, COS0_7, 1)
		BF0(tab, tabOffset, `val`, 8, 23, COS0_8, 1)
		/* pass 2 */
		BF(`val`, 7, 8, COS1_7, 4)
		BF(`val`, 23, 24, -COS1_7, 4)
		/* pass 3 */
		BF(`val`, 0, 7, COS2_0, 1)
		BF(`val`, 8, 15, -COS2_0, 1)
		BF(`val`, 16, 23, COS2_0, 1)
		BF(`val`, 24, 31, -COS2_0, 1)
		/* pass 1 */
		BF0(tab, tabOffset, `val`, 3, 28, COS0_3, 1)
		BF0(tab, tabOffset, `val`, 12, 19, COS0_12, 2)
		/* pass 2 */
		BF(`val`, 3, 12, COS1_3, 1)
		BF(`val`, 19, 28, -COS1_3, 1)
		/* pass 1 */
		BF0(tab, tabOffset, `val`, 4, 27, COS0_4, 1)
		BF0(tab, tabOffset, `val`, 11, 20, COS0_11, 2)
		/* pass 2 */
		BF(`val`, 4, 11, COS1_4, 1)
		BF(`val`, 20, 27, -COS1_4, 1)
		/* pass 3 */
		BF(`val`, 3, 4, COS2_3, 3)
		BF(`val`, 11, 12, -COS2_3, 3)
		BF(`val`, 19, 20, COS2_3, 3)
		BF(`val`, 27, 28, -COS2_3, 3)
		/* pass 4 */
		BF(`val`, 0, 3, COS3_0, 1)
		BF(`val`, 4, 7, -COS3_0, 1)
		BF(`val`, 8, 11, COS3_0, 1)
		BF(`val`, 12, 15, -COS3_0, 1)
		BF(`val`, 16, 19, COS3_0, 1)
		BF(`val`, 20, 23, -COS3_0, 1)
		BF(`val`, 24, 27, COS3_0, 1)
		BF(`val`, 28, 31, -COS3_0, 1)

		/* pass 1 */
		BF0(tab, tabOffset, `val`, 1, 30, COS0_1, 1)
		BF0(tab, tabOffset, `val`, 14, 17, COS0_14, 3)
		/* pass 2 */
		BF(`val`, 1, 14, COS1_1, 1)
		BF(`val`, 17, 30, -COS1_1, 1)
		/* pass 1 */
		BF0(tab, tabOffset, `val`, 6, 25, COS0_6, 1)
		BF0(tab, tabOffset, `val`, 9, 22, COS0_9, 1)
		/* pass 2 */
		BF(`val`, 6, 9, COS1_6, 2)
		BF(`val`, 22, 25, -COS1_6, 2)
		/* pass 3 */
		BF(`val`, 1, 6, COS2_1, 1)
		BF(`val`, 9, 14, -COS2_1, 1)
		BF(`val`, 17, 22, COS2_1, 1)
		BF(`val`, 25, 30, -COS2_1, 1)

		/* pass 1 */
		BF0(tab, tabOffset, `val`, 2, 29, COS0_2, 1)
		BF0(tab, tabOffset, `val`, 13, 18, COS0_13, 3)
		/* pass 2 */
		BF(`val`, 2, 13, COS1_2, 1)
		BF(`val`, 18, 29, -COS1_2, 1)
		/* pass 1 */
		BF0(tab, tabOffset, `val`, 5, 26, COS0_5, 1)
		BF0(tab, tabOffset, `val`, 10, 21, COS0_10, 1)
		/* pass 2 */
		BF(`val`, 5, 10, COS1_5, 2)
		BF(`val`, 21, 26, -COS1_5, 2)
		/* pass 3 */
		BF(`val`, 2, 5, COS2_2, 1)
		BF(`val`, 10, 13, -COS2_2, 1)
		BF(`val`, 18, 21, COS2_2, 1)
		BF(`val`, 26, 29, -COS2_2, 1)
		/* pass 4 */
		BF(`val`, 1, 2, COS3_1, 2)
		BF(`val`, 5, 6, -COS3_1, 2)
		BF(`val`, 9, 10, COS3_1, 2)
		BF(`val`, 13, 14, -COS3_1, 2)
		BF(`val`, 17, 18, COS3_1, 2)
		BF(`val`, 21, 22, -COS3_1, 2)
		BF(`val`, 25, 26, COS3_1, 2)
		BF(`val`, 29, 30, -COS3_1, 2)

		/* pass 5 */
		BF1(`val`, 0, 1, 2, 3)
		BF2(`val`, 4, 5, 6, 7)
		BF1(`val`, 8, 9, 10, 11)
		BF2(`val`, 12, 13, 14, 15)
		BF1(`val`, 16, 17, 18, 19)
		BF2(`val`, 20, 21, 22, 23)
		BF1(`val`, 24, 25, 26, 27)
		BF2(`val`, 28, 29, 30, 31)

		/* pass 6 */

		ADD(`val`, 8, 12)
		ADD(`val`, 12, 10)
		ADD(`val`, 10, 14)
		ADD(`val`, 14, 9)
		ADD(`val`, 9, 13)
		ADD(`val`, 13, 11)
		ADD(`val`, 11, 15)

		out[outOffset + 0] = `val`[0]
		out[outOffset + 16] = `val`[1]
		out[outOffset + 8] = `val`[2]
		out[outOffset + 24] = `val`[3]
		out[outOffset + 4] = `val`[4]
		out[outOffset + 20] = `val`[5]
		out[outOffset + 12] = `val`[6]
		out[outOffset + 28] = `val`[7]
		out[outOffset + 2] = `val`[8]
		out[outOffset + 18] = `val`[9]
		out[outOffset + 10] = `val`[10]
		out[outOffset + 26] = `val`[11]
		out[outOffset + 6] = `val`[12]
		out[outOffset + 22] = `val`[13]
		out[outOffset + 14] = `val`[14]
		out[outOffset + 30] = `val`[15]

		ADD(`val`, 24, 28)
		ADD(`val`, 28, 26)
		ADD(`val`, 26, 30)
		ADD(`val`, 30, 25)
		ADD(`val`, 25, 29)
		ADD(`val`, 29, 27)
		ADD(`val`, 27, 31)

		out[outOffset + 1] = `val`[16] + `val`[24]
		out[outOffset + 17] = `val`[17] + `val`[25]
		out[outOffset + 9] = `val`[18] + `val`[26]
		out[outOffset + 25] = `val`[19] + `val`[27]
		out[outOffset + 5] = `val`[20] + `val`[28]
		out[outOffset + 21] = `val`[21] + `val`[29]
		out[outOffset + 13] = `val`[22] + `val`[30]
		out[outOffset + 29] = `val`[23] + `val`[31]
		out[outOffset + 3] = `val`[24] + `val`[20]
		out[outOffset + 19] = `val`[25] + `val`[21]
		out[outOffset + 11] = `val`[26] + `val`[22]
		out[outOffset + 27] = `val`[27] + `val`[23]
		out[outOffset + 7] = `val`[28] + `val`[18]
		out[outOffset + 23] = `val`[29] + `val`[19]
		out[outOffset + 15] = `val`[30] + `val`[17]
		out[outOffset + 31] = `val`[31]
	}
}
