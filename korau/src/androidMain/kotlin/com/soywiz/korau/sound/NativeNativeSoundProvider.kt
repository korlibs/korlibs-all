package com.soywiz.korau.sound

import com.soywiz.korau.*

actual val nativeSoundProvider: NativeSoundProvider by lazy { AndroidNativeSoundProvider() }

