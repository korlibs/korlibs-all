/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2012-2018 DragonBones team and other contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.dragonbones.model

import com.dragonbones.core.*
import com.dragonbones.util.*

/**
 * - The DragonBones data.
 * A DragonBones data contains multiple armature data.
 * @see dragonBones.ArmatureData
 * @version DragonBones 3.0
 * @language en_US
 */
/**
 * - 龙骨数据。
 * 一个龙骨数据包含多个骨架数据。
 * @see dragonBones.ArmatureData
 * @version DragonBones 3.0
 * @language zh_CN
 */
class DragonBonesData  : BaseObject() {
	public override fun toString(): String {
		return "[class dragonBones.DragonBonesData]"
	}
	/**
	 * @private
	 */
	public var autoSearch: Boolean = false
	/**
	 * - The animation frame rate.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 动画帧频。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public var frameRate: Int = 0
	/**
	 * - The data version.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 数据版本。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public var version: String = ""
	/**
	 * - The DragonBones data name.
	 * The name is consistent with the DragonBones project name.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 龙骨数据名称。
	 * 该名称与龙骨项目名保持一致。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public var name: String = ""
	/**
	 * @private
	 */
	public var stage: ArmatureData? = null
	/**
	 * @internal
	 */
	public val frameIndices:  DoubleArray = []
	/**
	 * @internal
	 */
	public val cachedFrames:  DoubleArray = []
	/**
	 * - All armature data names.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 所有的骨架数据名称。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public val armatureNames: ArrayList<String> = arrayListOf()
	/**
	 * @private
	 */
	public val armatures: LinkedHashMap<String, ArmatureData> = LinkedHashMap<String, ArmatureData>()
	/**
	 * @internal
	 */
	public var binary: ArrayBuffer
	/**
	 * @internal
	 */
	public var intArray: Int16Array
	/**
	 * @internal
	 */
	public var floatArray: Float32Array
	/**
	 * @internal
	 */
	public var frameIntArray: ShortArray
	/**
	 * @internal
	 */
	public var frameFloatArray: Float32Array
	/**
	 * @internal
	 */
	public var frameArray: Int16Array
	/**
	 * @internal
	 */
	public var timelineArray: Uint16Array
	/**
	 * @internal
	 */
	public var colorArray: Uint16Array
	/**
	 * @private
	 */
	public var userData: UserData? = null // Initial value.

	protected fun _onClear(): Unit {
		for (a in this.armatures.values) a.returnToPool()
		this.armatures.clear()

		this.userData?.returnToPool()

		this.autoSearch = false
		this.frameRate = 0
		this.version = ""
		this.name = ""
		this.stage = null
		this.frameIndices.clear()
		this.cachedFrames.clear()
		this.armatureNames.clear()
		//this.armatures.clear();
		this.binary = null as any //
		this.intArray = null as any //
		this.floatArray = null as any //
		this.frameIntArray = null as any //
		this.frameFloatArray = null as any //
		this.frameArray = null as any //
		this.timelineArray = null as any //
		this.colorArray = null as any //
		this.userData = null
	}
	/**
	 * @internal
	 */
	public fun addArmature(value: ArmatureData): Unit {
		if (value.name in this.armatures) {
			console.warn("Same armature: " + value.name)
			return
		}

		value.parent = this
		this.armatures[value.name] = value
		this.armatureNames.add(value.name)
	}
	/**
	 * - Get a specific armature data.
	 * @param armatureName - The armature data name.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 获取特定的骨架数据。
	 * @param armatureName - 骨架数据名称。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public fun getArmature(armatureName: String): ArmatureData? {
		return if (armatureName in this.armatures) this.armatures[armatureName] else null
	}
}
