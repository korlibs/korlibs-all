package com.soywiz.korau.format.net.sourceforge.lame.mp3

/**
 * Structure to receive extracted header (toc may be null).

 * @author Ken
 */
class VBRTagData {
    var frames: Int = 0
    var headersize: Int = 0
    var encDelay: Int = 0
    var encPadding: Int = 0
    var hId: Int = 0
    var samprate: Int = 0
    var flags: Int = 0
    var bytes: Int = 0
    var vbrScale: Int = 0
    var toc = ByteArray(VBRTag.NUMTOCENTRIES)
}
