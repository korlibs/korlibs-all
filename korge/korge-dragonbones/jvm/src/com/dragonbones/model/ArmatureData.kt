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
import com.dragonbones.geom.*

/**
 * - The armature data.
 * @version DragonBones 3.0
 * @language en_US
 */
/**
 * - 骨架数据。
 * @version DragonBones 3.0
 * @language zh_CN
 */
class ArmatureData  : BaseObject() {
	public override fun toString(): String {
		return "[class dragonBones.ArmatureData]"
	}
	/**
	 * @private
	 */
	public var type: ArmatureType
	/**
	 * - The animation frame rate.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 动画帧率。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public var frameRate: Double
	/**
	 * @private
	 */
	public var cacheFrameRate: Double
	/**
	 * @private
	 */
	public var scale: Double
	/**
	 * - The armature name.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 骨架名称。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public var name: String
	/**
	 * @private
	 */
	public val aabb: Rectangle = new Rectangle()
	/**
	 * - The names of all the animation data.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 所有的动画数据名称。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public val animationNames: Array<String> = []
	/**
	 * @private
	 */
	public val sortedBones: Array<BoneData> = []
	/**
	 * @private
	 */
	public val sortedSlots: Array<SlotData> = []
	/**
	 * @private
	 */
	public val defaultActions: Array<ActionData> = []
	/**
	 * @private
	 */
	public val actions: Array<ActionData> = []
	/**
	 * @private
	 */
	public val bones: Map<BoneData> = {}
	/**
	 * @private
	 */
	public val slots: Map<SlotData> = {}
	/**
	 * @private
	 */
	public val constraints: Map<ConstraintData> = {}
	/**
	 * @private
	 */
	public val skins: Map<SkinData> = {}
	/**
	 * @private
	 */
	public val animations: Map<AnimationData> = {}
	/**
	 * - The default skin data.
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 默认插槽数据。
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	public var defaultSkin: SkinData?
	/**
	 * - The default animation data.
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 默认动画数据。
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	public var defaultAnimation: AnimationData?
	/**
	 * @private
	 */
	public var canvas: CanvasData? = null // Initial value.
	/**
	 * @private
	 */
	public var userData: UserData? = null // Initial value.
	/**
	 * @private
	 */
	public var parent: DragonBonesData

	protected fun _onClear(): Unit {
		for (action in this.defaultActions) {
			action.returnToPool()
		}

		for (action in this.actions) {
			action.returnToPool()
		}

		for (k in this.bones.keys) {
			this.bones[k].returnToPool()
			delete this.bones[k]
		}

		for (k in this.slots.keys) {
			this.slots[k].returnToPool()
			delete this.slots[k]
		}

		for (k in this.constraints.keys) {
			this.constraints[k].returnToPool()
			delete this.constraints[k]
		}

		for (k in this.skins.keys) {
			this.skins[k].returnToPool()
			delete this.skins[k]
		}

		for (let k in this.animations) {
			this.animations[k].returnToPool()
			delete this.animations[k]
		}

		if (this.canvas !== null) {
			this.canvas.returnToPool()
		}

		if (this.userData !== null) {
			this.userData.returnToPool()
		}

		this.type = ArmatureType.Armature
		this.frameRate = 0
		this.cacheFrameRate = 0
		this.scale = 1.0
		this.name = ""
		this.aabb.clear()
		this.animationNames.length = 0
		this.sortedBones.length = 0
		this.sortedSlots.length = 0
		this.defaultActions.length = 0
		this.actions.length = 0
		// this.bones.clear();
		// this.slots.clear();
		// this.constraints.clear();
		// this.skins.clear();
		// this.animations.clear();
		this.defaultSkin = null
		this.defaultAnimation = null
		this.canvas = null
		this.userData = null
		this.parent = null as any //
	}
	/**
	 * @internal
	 */
	public fun sortBones(): Unit {
		val total = this.sortedBones.size
		if (total <= 0) {
			return
		}

		val sortHelper = this.sortedBones.concat()
		var index = 0
		var count = 0
		this.sortedBones.size = 0
		while (count < total) {
			val bone = sortHelper[index++]
			if (index >= total) {
				index = 0
			}

			if (this.sortedBones.indexOf(bone) >= 0) {
				continue
			}

			var flag = false
			for (k in this.constraints) { // Wait constraint.
				val constraint = this.constraints[k]
				if (constraint.root === bone && this.sortedBones.indexOf(constraint.target) < 0) {
					flag = true
					break
				}
			}

			if (flag) {
				continue
			}

			if (bone.parent !== null && this.sortedBones.indexOf(bone.parent) < 0) { // Wait parent.
				continue
			}

			this.sortedBones.push(bone)
			count++
		}
	}
	/**
	 * @internal
	 */
	public fun cacheFrames(frameRate: Double): Unit {
		if (this.cacheFrameRate > 0) { // TODO clear cache.
			return
		}

		this.cacheFrameRate = frameRate
		for (k in this.animations.keys) {
			this.animations[k].cacheFrames(this.cacheFrameRate)
		}
	}
	/**
	 * @internal
	 */
	public fun setCacheFrame(globalTransformMatrix: Matrix, transform: Transform): Double {
		val dataArray = this.parent.cachedFrames
		var arrayOffset = dataArray.length

		dataArray.length += 10
		dataArray[arrayOffset] = globalTransformMatrix.a
		dataArray[arrayOffset + 1] = globalTransformMatrix.b
		dataArray[arrayOffset + 2] = globalTransformMatrix.c
		dataArray[arrayOffset + 3] = globalTransformMatrix.d
		dataArray[arrayOffset + 4] = globalTransformMatrix.tx
		dataArray[arrayOffset + 5] = globalTransformMatrix.ty
		dataArray[arrayOffset + 6] = transform.rotation
		dataArray[arrayOffset + 7] = transform.skew
		dataArray[arrayOffset + 8] = transform.scaleX
		dataArray[arrayOffset + 9] = transform.scaleY

		return arrayOffset
	}
	/**
	 * @internal
	 */
	public fun getCacheFrame(globalTransformMatrix: Matrix, transform: Transform, arrayOffset: Double): Unit {
		val dataArray = this.parent.cachedFrames
		globalTransformMatrix.a = dataArray[arrayOffset]
		globalTransformMatrix.b = dataArray[arrayOffset + 1]
		globalTransformMatrix.c = dataArray[arrayOffset + 2]
		globalTransformMatrix.d = dataArray[arrayOffset + 3]
		globalTransformMatrix.tx = dataArray[arrayOffset + 4]
		globalTransformMatrix.ty = dataArray[arrayOffset + 5]
		transform.rotation = dataArray[arrayOffset + 6]
		transform.skew = dataArray[arrayOffset + 7]
		transform.scaleX = dataArray[arrayOffset + 8]
		transform.scaleY = dataArray[arrayOffset + 9]
		transform.x = globalTransformMatrix.tx
		transform.y = globalTransformMatrix.ty
	}
	/**
	 * @internal
	 */
	public fun addBone(value: BoneData): Unit {
		if (value.name in this.bones) {
			console.warn("Same bone: " + value.name)
			return
		}

		this.bones[value.name] = value
		this.sortedBones.push(value)
	}
	/**
	 * @internal
	 */
	public fun addSlot(value: SlotData): Unit {
		if (value.name in this.slots) {
			console.warn("Same slot: " + value.name)
			return
		}

		this.slots[value.name] = value
		this.sortedSlots.push(value)
	}
	/**
	 * @internal
	 */
	public fun addConstraint(value: ConstraintData): Unit {
		if (value.name in this.constraints) {
			console.warn("Same constraint: " + value.name)
			return
		}

		this.constraints[value.name] = value
	}
	/**
	 * @internal
	 */
	public fun addSkin(value: SkinData): Unit {
		if (value.name in this.skins) {
			console.warn("Same skin: " + value.name)
			return
		}

		value.parent = this
		this.skins[value.name] = value
		if (this.defaultSkin === null) {
			this.defaultSkin = value
		}

		if (value.name === "default") {
			this.defaultSkin = value
		}
	}
	/**
	 * @internal
	 */
	public fun addAnimation(value: AnimationData): Unit {
		if (value.name in this.animations) {
			console.warn("Same animation: " + value.name)
			return
		}

		value.parent = this
		this.animations[value.name] = value
		this.animationNames.push(value.name)
		if (this.defaultAnimation === null) {
			this.defaultAnimation = value
		}
	}
	/**
	 * @internal
	 */
	public fun addAction(value: ActionData, isDefault: Boolean): Unit {
		if (isDefault) {
			this.defaultActions.push(value)
		}
		else {
			this.actions.push(value)
		}
	}
	/**
	 * - Get a specific done data.
	 * @param boneName - The bone name.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 获取特定的骨骼数据。
	 * @param boneName - 骨骼名称。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public fun getBone(boneName: String): BoneData? {
		return boneName in this.bones ? this.bones[boneName] : null
	}
	/**
	 * - Get a specific slot data.
	 * @param slotName - The slot name.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 获取特定的插槽数据。
	 * @param slotName - 插槽名称。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public fun getSlot(slotName: String): SlotData? {
		return slotName in this.slots ? this.slots[slotName] : null
	}
	/**
	 * @private
	 */
	public fun getConstraint(constraintName: String): ConstraintData? {
		return constraintName in this.constraints ? this.constraints[constraintName] : null
	}
	/**
	 * - Get a specific skin data.
	 * @param skinName - The skin name.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 获取特定皮肤数据。
	 * @param skinName - 皮肤名称。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public fun getSkin(skinName: String): SkinData? {
		return if (skinName in this.skins) this.skins[skinName] else null
	}
	/**
	 * @private
	 */
	public fun getMesh(skinName: String, slotName: String, meshName: String): MeshDisplayData? {
		val skin = this.getSkin(skinName)
		if (skin === null) {
			return null
		}

		return skin.getDisplay(slotName, meshName) as MeshDisplayData?
	}
	/**
	 * - Get a specific animation data.
	 * @param animationName - The animation animationName.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 获取特定的动画数据。
	 * @param animationName - 动画名称。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public fun getAnimation(animationName: String): AnimationData? {
		return animationName in this.animations ? this.animations[animationName] : null
	}
}
/**
 * - The bone data.
 * @version DragonBones 3.0
 * @language en_US
 */
/**
 * - 骨骼数据。
 * @version DragonBones 3.0
 * @language zh_CN
 */
class BoneData  :  BaseObject() {
	public override fun toString(): String {
		return "[class dragonBones.BoneData]"
	}
	/**
	 * @private
	 */
	public var inheritTranslation: Boolean
	/**
	 * @private
	 */
	public var inheritRotation: Boolean
	/**
	 * @private
	 */
	public var inheritScale: Boolean
	/**
	 * @private
	 */
	public var inheritReflection: Boolean
	/**
	 * @private
	 */
	public var type: BoneType
	/**
	 * - The bone length.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 骨骼长度。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public var length: Double
	/**
	 * @private
	 */
	public var alpha: Double
	/**
	 * - The bone name.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 骨骼名称。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public var name: String
	/**
	 * @private
	 */
	public val transform: Transform = new Transform()
	/**
	 * @private
	 */
	public var userData: UserData? = null // Initial value.
	/**
	 * - The parent bone data.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 父骨骼数据。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public var parent: BoneData?

	protected fun _onClear(): Unit {
		if (this.userData !== null) {
			this.userData.returnToPool()
		}

		this.inheritTranslation = false
		this.inheritRotation = false
		this.inheritScale = false
		this.inheritReflection = false
		this.type = BoneType.Bone
		this.length = 0.0
		this.alpha = 1.0
		this.name = ""
		this.transform.identity()
		this.userData = null
		this.parent = null
	}
}
/**
 * @internal
 */
class SurfaceData  :  BoneData {
	public override fun toString(): String {
		return "[class dragonBones.SurfaceData]"
	}

	public var segmentX: Double
	public var segmentY: Double
	public var readonly geometry: GeometryData = new GeometryData()

	protected fun _onClear(): Unit {
		super._onClear()

		this.type = BoneType.Surface
		this.segmentX = 0
		this.segmentY = 0
		this.geometry.clear()
	}
}
/**
 * - The slot data.
 * @version DragonBones 3.0
 * @language en_US
 */
/**
 * - 插槽数据。
 * @version DragonBones 3.0
 * @language zh_CN
 */
class SlotData  :  BaseObject() {
	companion object {
		/**
		 * @internal
		 */
		public val DEFAULT_COLOR: ColorTransform = ColorTransform()

		/**
		 * @internal
		 */
		public fun createColor(): ColorTransform {
			return ColorTransform()
		}

	}

	public override fun toString(): String {
		return "[class dragonBones.SlotData]"
	}
	/**
	 * @private
	 */
	public var blendMode: BlendMode
	/**
	 * @private
	 */
	public var displayIndex: Double
	/**
	 * @private
	 */
	public var zOrder: Double
	/**
	 * @private
	 */
	public var zIndex: Double
	/**
	 * @private
	 */
	public var alpha: Double
	/**
	 * - The slot name.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 插槽名称。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public var name: String
	/**
	 * @private
	 */
	public var color: ColorTransform = null as any // Initial value.
	/**
	 * @private
	 */
	public var userData: UserData? = null // Initial value.
	/**
	 * - The parent bone data.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 父骨骼数据。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public var parent: BoneData

	protected fun _onClear(): Unit {
		if (this.userData !== null) {
			this.userData.returnToPool()
		}

		this.blendMode = BlendMode.Normal
		this.displayIndex = 0
		this.zOrder = 0
		this.zIndex = 0
		this.alpha = 1.0
		this.name = ""
		this.color = null as any //
		this.userData = null
		this.parent = null as any //
	}
}
