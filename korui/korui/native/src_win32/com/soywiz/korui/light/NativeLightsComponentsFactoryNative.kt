package com.soywiz.korui.light

import com.soywiz.korag.*
import com.soywiz.kgl.*
import com.soywiz.korui.*
import kotlin.coroutines.experimental.*
import com.soywiz.korio.async.*
import com.soywiz.korui.*

actual object NativeLightsComponentsFactory : LightComponentsFactory {
	actual override fun create(context: CoroutineContext, nativeCtx: Any?): LightComponents = NativeLightComponents()
}

class NativeLightComponents : LightComponents() {
	override fun create(type: LightType): LightComponentInfo {
		var agg: AG? = null
		val handle: Any = when (type) {
			LightType.FRAME -> Any()
			LightType.CONTAINER -> Any()
			LightType.BUTTON -> Any()
			LightType.IMAGE -> Any()
			LightType.PROGRESS -> Any()
			LightType.LABEL -> Any()
			LightType.TEXT_FIELD -> Any()
			LightType.TEXT_AREA -> Any()
			LightType.CHECK_BOX -> Any()
			LightType.SCROLL_PANE -> Any()
			LightType.AGCANVAS -> {
				agg = AGOpenglFactory.create(Any()).create(Any())
				agg.nativeComponent
			}
			else -> throw UnsupportedOperationException("Type: $type")
		}
		return LightComponentInfo(handle).apply {
			if (agg != null) {
				this.ag = agg!!
			}
		}
	}
}
