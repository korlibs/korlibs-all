package com.soywiz.kgl

import com.soywiz.kmem.*
import java.nio.*

val KmlNativeBuffer.nioBuffer: java.nio.ByteBuffer get() = this.mem.buffer
