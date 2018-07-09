package com.soywiz.korui.light

import com.soywiz.korag.*
import com.soywiz.kgl.*
import com.soywiz.korui.*
import kotlin.coroutines.experimental.*
import com.soywiz.korio.async.*

actual object NativeLightsComponentsFactory : LightComponentsFactory {
	actual override fun create(context: CoroutineContext): LightComponents = NativeLightComponents(context)
}

class NativeLightComponents(val context: CoroutineContext) : LightComponents() {
	val eventLoop: MacosNativeEventLoop get() = (context.eventLoop as MacosNativeEventLoop)
	//val eventLoop: MacosNativeEventLoop get() = MacosNativeEventLoop

	override fun create(type: LightType): LightComponentInfo {
		var agg: AG? = null
		@Suppress("REDUNDANT_ELSE_IN_WHEN")
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
				agg = eventLoop.ag
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

	//protected override fun <T : Event> registerEventKind(c: Any, clazz: KClass<T>, ed: EventDispatcher): Closeable {
	//	return DummyCloseable
	//}
}
