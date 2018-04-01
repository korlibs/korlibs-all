package com.soywiz.korui.light.log

import com.soywiz.korui.light.LightComponents
import com.soywiz.korui.light.LightProperty
import com.soywiz.korui.light.LightType

class LogLightComponents : LightComponents() {
	val log = arrayListOf<String>()
	var lastId = 0

	override fun create(type: LightType): LightComponentInfo {
		val id = lastId++
		log += "create($type)=$id"
		return LightComponentInfo(id)
	}

	override fun setParent(c: Any, parent: Any?) {
		log += "setParent($c,$parent)"
	}

	override fun setBounds(c: Any, x: Int, y: Int, width: Int, height: Int) {
		log += "setBounds($c,$x,$y,$width,$height)"
	}

	override fun <T> setProperty(c: Any, key: LightProperty<T>, value: T) {
		log += "setProperty($c,$key,$value)"
	}
}