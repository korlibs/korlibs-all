package com.soywiz.korio.util

import com.soywiz.korio.vfs.*
import java.net.*

val URL.basename: String get() = PathInfo(this.file).basename