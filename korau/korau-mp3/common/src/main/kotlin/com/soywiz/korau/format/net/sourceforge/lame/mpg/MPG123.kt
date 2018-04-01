package com.soywiz.korau.format.net.sourceforge.lame.mpg

import com.soywiz.korio.JvmField

object MPG123 {
    const val M_SQRT2 = 1.41421356237309504880
    const val M_PI = 3.14159265358979323846

    const val SBLIMIT = 32
    const val SSLIMIT = 18

    //public static final int MPG_MD_STEREO = 0;
    const val MPG_MD_JOINT_STEREO = 1
    //public static final int MPG_MD_DUAL_CHANNEL = 2;
    const val MPG_MD_MONO = 3

    const val MAXFRAMESIZE = 2880

    /* AF: ADDED FOR LAYER1/LAYER2 */
    const val SCALE_BLOCK = 12

    internal class gr_info_s {
        @JvmField var scfsi: Int = 0
        @JvmField var part2_3_length: Int = 0
        @JvmField var big_values: Int = 0
        @JvmField var scalefac_compress: Int = 0
        @JvmField var block_type: Int = 0
        @JvmField var mixed_block_flag: Int = 0
        @JvmField var table_select = IntArray(3)
        @JvmField var maxband = IntArray(3)
        @JvmField var maxbandl: Int = 0
        @JvmField var maxb: Int = 0
        @JvmField var region1start: Int = 0
        @JvmField var region2start: Int = 0
        @JvmField var preflag: Int = 0
        @JvmField var scalefac_scale: Int = 0
        @JvmField var count1table_select: Int = 0
        @JvmField var full_gain = Array<FloatArray>(3) { floatArrayOf() }
        @JvmField var full_gainPos = IntArray(3)
        @JvmField var pow2gain: FloatArray = floatArrayOf()
        @JvmField var pow2gainPos: Int = 0
    }

    internal class grT {
        @JvmField var gr = Array<gr_info_s>(2) { gr_info_s() }

        init {
            gr[0] = gr_info_s()
            gr[1] = gr_info_s()
        }
    }

    internal class III_sideinfo {
        @JvmField var main_data_begin: Int = 0
        @JvmField var private_bits: Int = 0
        @JvmField var ch = Array<grT>(2) { grT() }

        init {
            ch[0] = grT()
            ch[1] = grT()
        }
    }
}
