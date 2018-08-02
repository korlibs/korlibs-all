package com.soywiz.korui.light

import kotlin.coroutines.experimental.*

actual object NativeLightsComponentsFactory : LightComponentsFactory {
	actual override fun create(context: CoroutineContext): LightComponents = HtmlLightComponents()
}
