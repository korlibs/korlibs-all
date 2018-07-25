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
package com.dragonbones.parser

import com.dragonbones.core.*
import com.dragonbones.model.*

/**
 * @private
 */
enum class FrameValueType {
	Step,
	Int,
	Float,
}
/**
 * @private
 */
open class ObjectDataParser :  DataParser() {
	companion object {


		protected fun _getBoolean(rawData: Any, key: String, defaultValue: Boolean): Boolean
		{
			if (key in rawData) {
				val value = rawData [key]
				val type = typeof value

				if (type === "boolean") {
					return value
				} else if (type === "string") {
					when(value) {
						"0",
						"NaN",
						"",
						"false",
						"null",
						"undefined" ->
						return false

						else ->
						return true
					}
				} else {
					return !!value
				}
			}

			return defaultValue
		}

		protected fun _getNumber(rawData: Any, key: String, defaultValue: Double): Double
		{
			if (key in rawData) {
				val value = rawData [key]
				if (value === null || value === "NaN") {
					return defaultValue
				}

				return +value || 0
			}

			return defaultValue
		}

		protected fun _getString(rawData: Any, key: String, defaultValue: String): String
		{
			if (key in rawData) {
				val value = rawData [key]
				val type = typeof value

				if (type === "string") {
					return value
				}

				return String(value)
			}

			return defaultValue
		}

		private var _objectDataParserInstance: ObjectDataParser? = null
		/**
		 * - Deprecated, please refer to {@link dragonBones.BaseFactory#parseDragonBonesData()}.
		 * @deprecated
		 * @language en_US
		 */
		/**
		 * - 已废弃，请参考 {@link dragonBones.BaseFactory#parseDragonBonesData()}。
		 * @deprecated
		 * @language zh_CN
		 */
		public fun getInstance(): ObjectDataParser {
			if (ObjectDataParser._objectDataParserInstance === null) {
				ObjectDataParser._objectDataParserInstance = new ObjectDataParser()
			}

			return ObjectDataParser._objectDataParserInstance
		}

	}

	protected var _rawTextureAtlasIndex: Double = 0
	protected var val _rawBones: Array<BoneData> = []
	protected var _data: DragonBonesData = null as any //
	protected var _armature: ArmatureData = null as any //
	protected var _bone: BoneData = null as any //
	protected var _geometry: GeometryData = null as any //
	protected var _slot: SlotData = null as any //
	protected var _skin: SkinData = null as any //
	protected var _mesh: MeshDisplayData = null as any //
	protected var _animation: AnimationData = null as any //
	protected var _timeline: TimelineData = null as any //
	protected var _rawTextureAtlases: Array<any>? = null

	private var _frameValueType: FrameValueType = FrameValueType.Step
	private var _defaultColorOffset: Double = -1
	private var _prevClockwise: Double = 0
	private var _prevRotation: Double = 0.0
	private var _frameDefaultValue: Double = 0.0
	private var _frameValueScale: Double = 1.0
	private val _helpMatrixA: Matrix = new Matrix()
	private val _helpMatrixB: Matrix = new Matrix()
	private val _helpTransform: Transform = new Transform()
	private val _helpColorTransform: ColorTransform = new ColorTransform()
	private val _helpPoint: Point = new Point()
	private val _helpArray:  DoubleArray = []
	private val _intArray:  DoubleArray = []
	private val _floatArray:  DoubleArray = []
	private val _frameIntArray:  DoubleArray = []
	private val _frameFloatArray:  DoubleArray = []
	private val _frameArray:  DoubleArray = []
	private val _timelineArray:  DoubleArray = []
	private val _colorArray:  DoubleArray = []
	private val _cacheRawMeshes: Array<any> = []
	private val _cacheMeshes: Array<MeshDisplayData> = []
	private val _actionFrames: Array<ActionFrame> = []
	private val _weightSlotPose: Map<Array<number>> = {}
	private val _weightBonePoses: Map<Array<number>> = {}
	private val _cacheBones: Map<Array<BoneData>> = {}
	private val _slotChildActions: Map<Array<ActionData>> = {}

	private fun _getCurvePoint(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double, x4: Double, y4: Double, t: Double, result: Point): Unit {
		val l_t = 1.0 - t
		val powA = l_t * l_t
		val powB = t * t
		val kA = l_t * powA
		val kB = 3.0 * t * powA
		val kC = 3.0 * l_t * powB
		val kD = t * powB

		result.x = kA * x1 + kB * x2 + kC * x3 + kD * x4
		result.y = kA * y1 + kB * y2 + kC * y3 + kD * y4
	}

	private fun _samplingEasingCurve(curve:  DoubleArray, samples:  DoubleArray): Boolean {
		val curveCount = curve.size

		if (curveCount % 3 == 1) {
			var stepIndex = -2
			for (i in 0 until samples.size) {
				var t = (i + 1) / (l + 1) // float
				while ((stepIndex + 6 < curveCount ? curve[stepIndex + 6] : 1) < t) { // stepIndex + 3 * 2
					stepIndex += 6
			}

				val isInCurve = stepIndex >= 0 && stepIndex + 6 < curveCount
				val x1 = if (isInCurve) curve[stepIndex] else 0.0
				val y1 = if (isInCurve) curve[stepIndex + 1] else 0.0
				val x2 = curve[stepIndex + 2]
				val y2 = curve[stepIndex + 3]
				val x3 = curve[stepIndex + 4]
				val y3 = curve[stepIndex + 5]
				val x4 = isInCurve ? curve[stepIndex + 6] : 1.0
				val y4 = isInCurve ? curve[stepIndex + 7] : 1.0

				var lower = 0.0
				var higher = 1.0
				while (higher - lower > 0.0001) {
					val percentage = (higher + lower) * 0.5
					this._getCurvePoint(x1, y1, x2, y2, x3, y3, x4, y4, percentage, this._helpPoint)
					if (t - this._helpPoint.x > 0.0) {
						lower = percentage
					}
					else {
						higher = percentage
					}
				}

				samples[i] = this._helpPoint.y
			}

			return true
		}
		else {
			var stepIndex = 0
			for (i in 0 until samples.size) {
				var t = (i + 1) / (l + 1) // float
				while (curve[stepIndex + 6] < t) { // stepIndex + 3 * 2
					stepIndex += 6
				}

				val x1 = curve[stepIndex]
				val y1 = curve[stepIndex + 1]
				val x2 = curve[stepIndex + 2]
				val y2 = curve[stepIndex + 3]
				val x3 = curve[stepIndex + 4]
				val y3 = curve[stepIndex + 5]
				val x4 = curve[stepIndex + 6]
				val y4 = curve[stepIndex + 7]

				var lower = 0.0
				var higher = 1.0
				while (higher - lower > 0.0001) {
					val percentage = (higher + lower) * 0.5
					this._getCurvePoint(x1, y1, x2, y2, x3, y3, x4, y4, percentage, this._helpPoint)
					if (t - this._helpPoint.x > 0.0) {
						lower = percentage
					}
					else {
						higher = percentage
					}
				}

				samples[i] = this._helpPoint.y
			}

			return false
		}
	}

	private fun _parseActionDataInFrame(rawData: Any, frameStart: Double, bone: BoneData?, slot: SlotData?): Unit {
		if (DataParser.EVENT in rawData) {
			this._mergeActionFrame(rawData[DataParser.EVENT], frameStart, ActionType.Frame, bone, slot)
		}

		if (DataParser.SOUND in rawData) {
			this._mergeActionFrame(rawData[DataParser.SOUND], frameStart, ActionType.Sound, bone, slot)
		}

		if (DataParser.ACTION in rawData) {
			this._mergeActionFrame(rawData[DataParser.ACTION], frameStart, ActionType.Play, bone, slot)
		}

		if (DataParser.EVENTS in rawData) {
			this._mergeActionFrame(rawData[DataParser.EVENTS], frameStart, ActionType.Frame, bone, slot)
		}

		if (DataParser.ACTIONS in rawData) {
			this._mergeActionFrame(rawData[DataParser.ACTIONS], frameStart, ActionType.Play, bone, slot)
		}
	}

	private fun _mergeActionFrame(rawData: Any, frameStart: Double, type: ActionType, bone: BoneData?, slot: SlotData?): Unit {
		val actionOffset = this._armature.actions.length
		val actions = this._parseActionData(rawData, type, bone, slot)
		var frameIndex = 0
		var frame: ActionFrame? = null

		for (action in actions) {
			this._armature.addAction(action, false)
		}

		if (this._actionFrames.length === 0) { // First frame.
			frame = new ActionFrame()
			frame.frameStart = 0
			this._actionFrames.push(frame)
			frame = null
		}

		for (eachFrame in this._actionFrames) { // Get same frame.
			if (eachFrame.frameStart === frameStart) {
				frame = eachFrame
				break
			}
			else if (eachFrame.frameStart > frameStart) {
				break
			}

			frameIndex++
		}

		if (frame === null) { // Create and cache frame.
			frame = new ActionFrame()
			frame.frameStart = frameStart
			this._actionFrames.splice(frameIndex, 0, frame)
		}

		for (var i = 0; i < actions.length; ++i) { // Cache action offsets.
			frame.actions.push(actionOffset + i)
		}
	}

	protected fun _parseArmature(rawData: Any, scale: Double): ArmatureData {
		val armature = BaseObject.borrowObject<ArmatureData>()
		armature.name = ObjectDataParser._getString(rawData, DataParser.NAME, "")
		armature.frameRate = ObjectDataParser._getNumber(rawData, DataParser.FRAME_RATE, this._data.frameRate)
		armature.scale = scale

		if (DataParser.TYPE in rawData && typeof rawData[DataParser.TYPE] === "string") {
			armature.type = DataParser._getArmatureType(rawData[DataParser.TYPE])
		}
		else {
			armature.type = ObjectDataParser._getNumber(rawData, DataParser.TYPE, ArmatureType.Armature)
		}

		if (armature.frameRate === 0) { // Data error.
			armature.frameRate = 24
		}

		this._armature = armature

		if (DataParser.CANVAS in rawData) {
			val rawCanvas = rawData[DataParser.CANVAS]
			val canvas = BaseObject.borrowObject<CanvasData>()

			if (DataParser.COLOR in rawCanvas) {
				canvas.hasBackground = true
			}
			else {
				canvas.hasBackground = false
			}

			canvas.color = ObjectDataParser._getNumber(rawCanvas, DataParser.COLOR, 0)
			canvas.x = ObjectDataParser._getNumber(rawCanvas, DataParser.X, 0) * armature.scale
			canvas.y = ObjectDataParser._getNumber(rawCanvas, DataParser.Y, 0) * armature.scale
			canvas.width = ObjectDataParser._getNumber(rawCanvas, DataParser.WIDTH, 0) * armature.scale
			canvas.height = ObjectDataParser._getNumber(rawCanvas, DataParser.HEIGHT, 0) * armature.scale
			armature.canvas = canvas
		}

		if (DataParser.AABB in rawData) {
			val rawAABB = rawData[DataParser.AABB]
			armature.aabb.x = ObjectDataParser._getNumber(rawAABB, DataParser.X, 0.0) * armature.scale
			armature.aabb.y = ObjectDataParser._getNumber(rawAABB, DataParser.Y, 0.0) * armature.scale
			armature.aabb.width = ObjectDataParser._getNumber(rawAABB, DataParser.WIDTH, 0.0) * armature.scale
			armature.aabb.height = ObjectDataParser._getNumber(rawAABB, DataParser.HEIGHT, 0.0) * armature.scale
		}

		if (DataParser.BONE in rawData) {
			val rawBones = rawData[DataParser.BONE] as Array<any>
			for (rawBone in rawBones) {
				val parentName = ObjectDataParser._getString(rawBone, DataParser.PARENT, "")
				val bone = this._parseBone(rawBone)

				if (parentName.length > 0) { // Get bone parent.
					val parent = armature.getBone(parentName)
					if (parent !== null) {
						bone.parent = parent
					}
					else { // Cache.
						if (!(parentName in this._cacheBones)) {
							this._cacheBones[parentName] = []
						}

						this._cacheBones[parentName].push(bone)
					}
				}

				if (bone.name in this._cacheBones) {
					for (child in this._cacheBones[bone.name]) {
						child.parent = bone
					}

					delete this._cacheBones[bone.name]
				}

				armature.addBone(bone)
				this._rawBones.push(bone) // Cache raw bones sort.
			}
		}

		if (DataParser.IK in rawData) {
			val rawIKS = rawData[DataParser.IK] as Array<any>
			for (rawIK in rawIKS) {
				val constraint = this._parseIKConstraint(rawIK)
				if (constraint) {
					armature.addConstraint(constraint)
				}
			}
		}

		armature.sortBones()

		if (DataParser.SLOT in rawData) {
			var zOrder = 0
			val rawSlots = rawData[DataParser.SLOT] as Array<any>
			for (rawSlot in rawSlots) {
				armature.addSlot(this._parseSlot(rawSlot, zOrder++))
			}
		}

		if (DataParser.SKIN in rawData) {
			val rawSkins = rawData[DataParser.SKIN] as Array<any>
			for (rawSkin in rawSkins) {
				armature.addSkin(this._parseSkin(rawSkin))
			}
		}

		if (DataParser.PATH_CONSTRAINT in rawData) {
			val rawPaths = rawData[DataParser.PATH_CONSTRAINT] as Array<any>
			for (rawPath in rawPaths) {
				val constraint = this._parsePathConstraint(rawPath)
				if (constraint) {
					armature.addConstraint(constraint)
				}
			}
		}

		for (var i = 0, l = this._cacheRawMeshes.length; i < l; ++i) { // Link mesh.
			val rawData = this._cacheRawMeshes[i]
			val shareName = ObjectDataParser._getString(rawData, DataParser.SHARE, "")
			if (shareName.length === 0) {
				continue
			}

			var skinName = ObjectDataParser._getString(rawData, DataParser.SKIN, DataParser.DEFAULT_NAME)
			if (skinName.length === 0) { //
				skinName = DataParser.DEFAULT_NAME
			}

			val shareMesh = armature.getMesh(skinName, "", shareName) as MeshDisplayData? // TODO slot;
			if (shareMesh === null) {
				continue // Error.
			}

			val mesh = this._cacheMeshes[i]
			mesh.geometry.shareFrom(shareMesh.geometry)
		}

		if (DataParser.ANIMATION in rawData) {
			val rawAnimations = rawData[DataParser.ANIMATION] as Array<any>
			for (rawAnimation in rawAnimations) {
				val animation = this._parseAnimation(rawAnimation)
				armature.addAnimation(animation)
			}
		}

		if (DataParser.DEFAULT_ACTIONS in rawData) {
			val actions = this._parseActionData(rawData[DataParser.DEFAULT_ACTIONS], ActionType.Play, null, null)
			for (action in actions) {
				armature.addAction(action, true)

				if (action.type === ActionType.Play) { // Set default animation from default action.
					val animation = armature.getAnimation(action.name)
					if (animation !== null) {
						armature.defaultAnimation = animation
					}
				}
			}
		}

		if (DataParser.ACTIONS in rawData) {
			val actions = this._parseActionData(rawData[DataParser.ACTIONS], ActionType.Play, null, null)
			for (action in actions) {
				armature.addAction(action, false)
			}
		}

		// Clear helper.
		this._rawBones.length = 0
		this._cacheRawMeshes.length = 0
		this._cacheMeshes.length = 0
		this._armature = null as any

		for (var k in this._weightSlotPose) {
			delete this._weightSlotPose[k]
		}
		for (var k in this._weightBonePoses) {
			delete this._weightBonePoses[k]
		}
		for (var k in this._cacheBones) {
			delete this._cacheBones[k]
		}
		for (var k in this._slotChildActions) {
			delete this._slotChildActions[k]
		}

		return armature
	}

	protected _parseBone(rawData: Any): BoneData {
		var type: BoneType = BoneType.Bone

		if (DataParser.TYPE in rawData && typeof rawData[DataParser.TYPE] === "string") {
			type = DataParser._getBoneType(rawData[DataParser.TYPE])
		}
		else {
			type = ObjectDataParser._getNumber(rawData, DataParser.TYPE, BoneType.Bone)
		}

		if (type === BoneType.Bone) {
			val scale = this._armature.scale
			val bone = BaseObject.borrowObject<BoneData>()
			bone.inheritTranslation = ObjectDataParser._getBoolean(rawData, DataParser.INHERIT_TRANSLATION, true)
			bone.inheritRotation = ObjectDataParser._getBoolean(rawData, DataParser.INHERIT_ROTATION, true)
			bone.inheritScale = ObjectDataParser._getBoolean(rawData, DataParser.INHERIT_SCALE, true)
			bone.inheritReflection = ObjectDataParser._getBoolean(rawData, DataParser.INHERIT_REFLECTION, true)
			bone.length = ObjectDataParser._getNumber(rawData, DataParser.LENGTH, 0) * scale
			bone.alpha = ObjectDataParser._getNumber(rawData, DataParser.ALPHA, 1.0)
			bone.name = ObjectDataParser._getString(rawData, DataParser.NAME, "")

			if (DataParser.TRANSFORM in rawData) {
				this._parseTransform(rawData[DataParser.TRANSFORM], bone.transform, scale)
			}

			return bone
		}

		val surface = BaseObject.borrowObject<SurfaceData>()
		surface.alpha = ObjectDataParser._getNumber(rawData, DataParser.ALPHA, 1.0)
		surface.name = ObjectDataParser._getString(rawData, DataParser.NAME, "")
		surface.segmentX = ObjectDataParser._getNumber(rawData, DataParser.SEGMENT_X, 0)
		surface.segmentY = ObjectDataParser._getNumber(rawData, DataParser.SEGMENT_Y, 0)
		this._parseGeometry(rawData, surface.geometry)

		return surface
	}

	protected _parseIKConstraint(rawData: Any): ConstraintData? {
		val bone = this._armature.getBone(ObjectDataParser._getString(rawData, DataParser.BONE, ""))
		if (bone === null) {
			return null
		}

		val target = this._armature.getBone(ObjectDataParser._getString(rawData, DataParser.TARGET, ""))
		if (target === null) {
			return null
		}

		val chain = ObjectDataParser._getNumber(rawData, DataParser.CHAIN, 0)
		val constraint = BaseObject.borrowObject<IKConstraintData>()
		constraint.scaleEnabled = ObjectDataParser._getBoolean(rawData, DataParser.SCALE, false)
		constraint.bendPositive = ObjectDataParser._getBoolean(rawData, DataParser.BEND_POSITIVE, true)
		constraint.weight = ObjectDataParser._getNumber(rawData, DataParser.WEIGHT, 1.0)
		constraint.name = ObjectDataParser._getString(rawData, DataParser.NAME, "")
		constraint.type = ConstraintType.IK
		constraint.target = target

		if (chain > 0 && bone.parent !== null) {
			constraint.root = bone.parent
			constraint.bone = bone
		}
		else {
			constraint.root = bone
			constraint.bone = null
		}

		return constraint
	}

	protected _parsePathConstraint(rawData: Any): ConstraintData? {
		val target = this._armature.getSlot(ObjectDataParser._getString(rawData, DataParser.TARGET, ""))
		if (target === null) {
			return null
		}

		val defaultSkin = this._armature.defaultSkin
		if (defaultSkin === null) {
			return null
		}
		//TODO
		val targetDisplay = defaultSkin.getDisplay(target.name, ObjectDataParser._getString(rawData, DataParser.TARGET_DISPLAY, target.name))
		if (targetDisplay === null || !(targetDisplay instanceof PathDisplayData)) {
			return null
		}

		val bones = rawData[DataParser.BONES] as Array<string>
		if (bones === null || bones.length === 0) {
			return null
		}

		val constraint = BaseObject.borrowObject<PathConstraintData>()
		constraint.name = ObjectDataParser._getString(rawData, DataParser.NAME, "")
		constraint.type = ConstraintType.Path
		constraint.pathSlot = target
		constraint.pathDisplayData = targetDisplay
		constraint.target = target.parent
		constraint.positionMode = DataParser._getPositionMode(ObjectDataParser._getString(rawData, DataParser.POSITION_MODE, ""))
		constraint.spacingMode = DataParser._getSpacingMode(ObjectDataParser._getString(rawData, DataParser.SPACING_MODE, ""))
		constraint.rotateMode = DataParser._getRotateMode(ObjectDataParser._getString(rawData, DataParser.ROTATE_MODE, ""))
		constraint.position = ObjectDataParser._getNumber(rawData, DataParser.POSITION, 0)
		constraint.spacing = ObjectDataParser._getNumber(rawData, DataParser.SPACING, 0)
		constraint.rotateOffset = ObjectDataParser._getNumber(rawData, DataParser.ROTATE_OFFSET, 0)
		constraint.rotateMix = ObjectDataParser._getNumber(rawData, DataParser.ROTATE_MIX, 1)
		constraint.translateMix = ObjectDataParser._getNumber(rawData, DataParser.TRANSLATE_MIX, 1)
		//
		for (var boneName of bones) {
			val bone = this._armature.getBone(boneName)
			if (bone !== null) {
				constraint.AddBone(bone)

				if (constraint.root === null) {
					constraint.root = bone
				}
			}
		}

		return constraint
	}

	protected _parseSlot(rawData: Any, zOrder: Double): SlotData {
		val slot = BaseObject.borrowObject<SlotData>()
		slot.displayIndex = ObjectDataParser._getNumber(rawData, DataParser.DISPLAY_INDEX, 0)
		slot.zOrder = zOrder
		slot.zIndex = ObjectDataParser._getNumber(rawData, DataParser.Z_INDEX, 0)
		slot.alpha = ObjectDataParser._getNumber(rawData, DataParser.ALPHA, 1.0)
		slot.name = ObjectDataParser._getString(rawData, DataParser.NAME, "")
		slot.parent = this._armature.getBone(ObjectDataParser._getString(rawData, DataParser.PARENT, "")) as any //

		if (DataParser.BLEND_MODE in rawData && typeof rawData[DataParser.BLEND_MODE] === "string") {
			slot.blendMode = DataParser._getBlendMode(rawData[DataParser.BLEND_MODE])
		}
		else {
			slot.blendMode = ObjectDataParser._getNumber(rawData, DataParser.BLEND_MODE, BlendMode.Normal)
		}

		if (DataParser.COLOR in rawData) {
			slot.color = SlotData.createColor()
			this._parseColorTransform(rawData[DataParser.COLOR], slot.color)
		}
		else {
			slot.color = SlotData.DEFAULT_COLOR
		}

		if (DataParser.ACTIONS in rawData) {
			this._slotChildActions[slot.name] = this._parseActionData(rawData[DataParser.ACTIONS], ActionType.Play, null, null)
		}

		return slot
	}

	protected _parseSkin(rawData: Any): SkinData {
		val skin = BaseObject.borrowObject<SkinData>()
		skin.name = ObjectDataParser._getString(rawData, DataParser.NAME, DataParser.DEFAULT_NAME)

		if (skin.name.length === 0) {
			skin.name = DataParser.DEFAULT_NAME
		}

		if (DataParser.SLOT in rawData) {
			val rawSlots = rawData[DataParser.SLOT]
			this._skin = skin

			for (rawSlot in rawSlots) {
				val slotName = ObjectDataParser._getString(rawSlot, DataParser.NAME, "")
				val slot = this._armature.getSlot(slotName)

				if (slot !== null) {
					this._slot = slot

					if (DataParser.DISPLAY in rawSlot) {
						val rawDisplays = rawSlot[DataParser.DISPLAY]
						for (rawDisplay in rawDisplays) {
							if (rawDisplay) {
								skin.addDisplay(slotName, this._parseDisplay(rawDisplay))
							}
							else {
								skin.addDisplay(slotName, null)
							}
						}
					}

					this._slot = null as any //
				}
			}

			this._skin = null as any //
		}

		return skin
	}

	protected _parseDisplay(rawData: Any): DisplayData? {
		val name = ObjectDataParser._getString(rawData, DataParser.NAME, "")
		val path = ObjectDataParser._getString(rawData, DataParser.PATH, "")
		var type = DisplayType.Image
		var display: DisplayData? = null

		if (DataParser.TYPE in rawData && typeof rawData[DataParser.TYPE] === "string") {
			type = DataParser._getDisplayType(rawData[DataParser.TYPE])
		}
		else {
			type = ObjectDataParser._getNumber(rawData, DataParser.TYPE, type)
		}

		switch (type) {
			case DisplayType.Image: {
				val imageDisplay = display = BaseObject.borrowObject<ImageDisplayData>()
				imageDisplay.name = name
				imageDisplay.path = path.length > 0 ? path : name
				this._parsePivot(rawData, imageDisplay)
				break
			}

			case DisplayType.Armature: {
				val armatureDisplay = display = BaseObject.borrowObject<ArmatureDisplayData>()
				armatureDisplay.name = name
				armatureDisplay.path = path.length > 0 ? path : name
				armatureDisplay.inheritAnimation = true

				if (DataParser.ACTIONS in rawData) {
					val actions = this._parseActionData(rawData[DataParser.ACTIONS], ActionType.Play, null, null)
					for (action in actions) {
						armatureDisplay.addAction(action)
					}
				}
				else if (this._slot.name in this._slotChildActions) {
					val displays = this._skin.getDisplays(this._slot.name)
					if (displays === null ? this._slot.displayIndex === 0 : this._slot.displayIndex === displays.length) {
						for (action in this._slotChildActions[this._slot.name]) {
							armatureDisplay.addAction(action)
						}

						delete this._slotChildActions[this._slot.name]
					}
				}
				break
			}

			case DisplayType.Mesh: {
				val meshDisplay = display = BaseObject.borrowObject<MeshDisplayData>()
				meshDisplay.geometry.inheritDeform = ObjectDataParser._getBoolean(rawData, DataParser.INHERIT_DEFORM, true)
				meshDisplay.name = name
				meshDisplay.path = path.length > 0 ? path : name

				if (DataParser.SHARE in rawData) {
					meshDisplay.geometry.data = this._data
					this._cacheRawMeshes.push(rawData)
					this._cacheMeshes.push(meshDisplay)
				}
				else {
					this._parseMesh(rawData, meshDisplay)
				}
				break
			}

			case DisplayType.BoundingBox: {
				val boundingBox = this._parseBoundingBox(rawData)
				if (boundingBox !== null) {
					val boundingBoxDisplay = display = BaseObject.borrowObject<BoundingBoxDisplayData>()
					boundingBoxDisplay.name = name
					boundingBoxDisplay.path = path.length > 0 ? path : name
					boundingBoxDisplay.boundingBox = boundingBox
				}
				break
			}

			case DisplayType.Path: {
				val rawCurveLengths = rawData[DataParser.LENGTHS] as  DoubleArray
				val pathDisplay = display = BaseObject.borrowObject<PathDisplayData>()
				pathDisplay.closed = ObjectDataParser._getBoolean(rawData, DataParser.CLOSED, false)
				pathDisplay.constantSpeed = ObjectDataParser._getBoolean(rawData, DataParser.CONSTANT_SPEED, false)
				pathDisplay.name = name
				pathDisplay.path = path.length > 0 ? path : name
				pathDisplay.curveLengths.length = rawCurveLengths.length

				for (var i = 0, l = rawCurveLengths.length; i < l; ++i) {
					pathDisplay.curveLengths[i] = rawCurveLengths[i]
			}

				this._parsePath(rawData, pathDisplay)
				break
			}
		}

		if (display !== null && DataParser.TRANSFORM in rawData) {
			this._parseTransform(rawData[DataParser.TRANSFORM], display.transform, this._armature.scale)
		}

		return display
	}

	protected _parsePath(rawData: Any, display: PathDisplayData) {
		this._parseGeometry(rawData, display.geometry)
	}

	protected _parsePivot(rawData: Any, display: ImageDisplayData): Unit {
		if (DataParser.PIVOT in rawData) {
			val rawPivot = rawData[DataParser.PIVOT]
			display.pivot.x = ObjectDataParser._getNumber(rawPivot, DataParser.X, 0.0)
			display.pivot.y = ObjectDataParser._getNumber(rawPivot, DataParser.Y, 0.0)
		}
		else {
			display.pivot.x = 0.5
			display.pivot.y = 0.5
		}
	}

	protected _parseMesh(rawData: Any, mesh: MeshDisplayData): Unit {
		this._parseGeometry(rawData, mesh.geometry)

		if (DataParser.WEIGHTS in rawData) { // Cache pose data.
			val rawSlotPose = rawData[DataParser.SLOT_POSE] as  DoubleArray
			val rawBonePoses = rawData[DataParser.BONE_POSE] as  DoubleArray
			val meshName = this._skin.name + "_" + this._slot.name + "_" + mesh.name
			this._weightSlotPose[meshName] = rawSlotPose
			this._weightBonePoses[meshName] = rawBonePoses
		}
	}

	protected _parseBoundingBox(rawData: Any): BoundingBoxData? {
		var boundingBox: BoundingBoxData? = null
		var type = BoundingBoxType.Rectangle

		if (DataParser.SUB_TYPE in rawData && typeof rawData[DataParser.SUB_TYPE] === "string") {
			type = DataParser._getBoundingBoxType(rawData[DataParser.SUB_TYPE])
		}
		else {
			type = ObjectDataParser._getNumber(rawData, DataParser.SUB_TYPE, type)
		}

		switch (type) {
			case BoundingBoxType.Rectangle:
				boundingBox = BaseObject.borrowObject<RectangleBoundingBoxData>()
			break

			case BoundingBoxType.Ellipse:
				boundingBox = BaseObject.borrowObject<EllipseBoundingBoxData>()
			break

			case BoundingBoxType.Polygon:
				boundingBox = this._parsePolygonBoundingBox(rawData)
			break
		}

		if (boundingBox !== null) {
			boundingBox.color = ObjectDataParser._getNumber(rawData, DataParser.COLOR, 0x000000)
			if (boundingBox.type === BoundingBoxType.Rectangle || boundingBox.type === BoundingBoxType.Ellipse) {
				boundingBox.width = ObjectDataParser._getNumber(rawData, DataParser.WIDTH, 0.0)
				boundingBox.height = ObjectDataParser._getNumber(rawData, DataParser.HEIGHT, 0.0)
			}
		}

		return boundingBox
	}

	protected _parsePolygonBoundingBox(rawData: Any): PolygonBoundingBoxData {
		val polygonBoundingBox = BaseObject.borrowObject<PolygonBoundingBoxData>()

		if (DataParser.VERTICES in rawData) {
			val scale = this._armature.scale
			val rawVertices = rawData[DataParser.VERTICES] as  DoubleArray
			val vertices = polygonBoundingBox.vertices
			vertices.length = rawVertices.length

			for (var i = 0, l = rawVertices.length; i < l; i += 2) {
				val x = rawVertices[i] * scale
				val y = rawVertices[i + 1] * scale
				vertices[i] = x
				vertices[i + 1] = y

				// AABB.
				if (i === 0) {
					polygonBoundingBox.x = x
					polygonBoundingBox.y = y
					polygonBoundingBox.width = x
					polygonBoundingBox.height = y
				}
				else {
					if (x < polygonBoundingBox.x) {
						polygonBoundingBox.x = x
					}
					else if (x > polygonBoundingBox.width) {
						polygonBoundingBox.width = x
					}

					if (y < polygonBoundingBox.y) {
						polygonBoundingBox.y = y
					}
					else if (y > polygonBoundingBox.height) {
						polygonBoundingBox.height = y
					}
				}
			}

			polygonBoundingBox.width -= polygonBoundingBox.x
			polygonBoundingBox.height -= polygonBoundingBox.y
		}
		else {
			console.warn("Data error.\n Please reexport DragonBones Data to fixed the bug.")
		}

		return polygonBoundingBox
	}

	protected _parseAnimation(rawData: Any): AnimationData {
		val animation = BaseObject.borrowObject<AnimationData>()
		animation.blendType = DataParser._getAnimationBlendType(ObjectDataParser._getString(rawData, DataParser.BLEND_TYPE, ""))
		animation.frameCount = ObjectDataParser._getNumber(rawData, DataParser.DURATION, 0)
		animation.playTimes = ObjectDataParser._getNumber(rawData, DataParser.PLAY_TIMES, 1)
		animation.duration = animation.frameCount / this._armature.frameRate // float
		animation.fadeInTime = ObjectDataParser._getNumber(rawData, DataParser.FADE_IN_TIME, 0.0)
		animation.scale = ObjectDataParser._getNumber(rawData, DataParser.SCALE, 1.0)
		animation.name = ObjectDataParser._getString(rawData, DataParser.NAME, DataParser.DEFAULT_NAME)

		if (animation.name.length === 0) {
			animation.name = DataParser.DEFAULT_NAME
		}

		animation.frameIntOffset = this._frameIntArray.length
		animation.frameFloatOffset = this._frameFloatArray.length
		animation.frameOffset = this._frameArray.length
		this._animation = animation

		if (DataParser.FRAME in rawData) {
			val rawFrames = rawData[DataParser.FRAME] as Array<any>
			val keyFrameCount = rawFrames.length

			if (keyFrameCount > 0) {
				for (var i = 0, frameStart = 0; i < keyFrameCount; ++i) {
					val rawFrame = rawFrames[i]
					this._parseActionDataInFrame(rawFrame, frameStart, null, null)
					frameStart += ObjectDataParser._getNumber(rawFrame, DataParser.DURATION, 1)
				}
			}
		}

		if (DataParser.Z_ORDER in rawData) {
			this._animation.zOrderTimeline = this._parseTimeline(
				rawData[DataParser.Z_ORDER], null, DataParser.FRAME, TimelineType.ZOrder,
				FrameValueType.Step, 0,
				this._parseZOrderFrame
			)
		}

		if (DataParser.BONE in rawData) {
			val rawTimelines = rawData[DataParser.BONE] as Array<any>
			for (rawTimeline in rawTimelines) {
				this._parseBoneTimeline(rawTimeline)
			}
		}

		if (DataParser.SLOT in rawData) {
			val rawTimelines = rawData[DataParser.SLOT] as Array<any>
			for (rawTimeline in rawTimelines) {
				this._parseSlotTimeline(rawTimeline)
			}
		}

		if (DataParser.FFD in rawData) {
			val rawTimelines = rawData[DataParser.FFD] as Array<any>
			for (rawTimeline in rawTimelines) {
				var skinName = ObjectDataParser._getString(rawTimeline, DataParser.SKIN, DataParser.DEFAULT_NAME)
				val slotName = ObjectDataParser._getString(rawTimeline, DataParser.SLOT, "")
				val displayName = ObjectDataParser._getString(rawTimeline, DataParser.NAME, "")

				if (skinName.length === 0) { //
					skinName = DataParser.DEFAULT_NAME
				}

				this._slot = this._armature.getSlot(slotName) as any
				this._mesh = this._armature.getMesh(skinName, slotName, displayName) as any
				if (this._slot === null || this._mesh === null) {
					continue
				}

				val timeline = this._parseTimeline(
					rawTimeline, null, DataParser.FRAME, TimelineType.SlotDeform,
					FrameValueType.Float, 0,
					this._parseSlotDeformFrame
				)

				if (timeline !== null) {
					this._animation.addSlotTimeline(slotName, timeline)
				}

				this._slot = null as any //
				this._mesh = null as any //
			}
		}

		if (DataParser.IK in rawData) {
			val rawTimelines = rawData[DataParser.IK] as Array<any>
			for (rawTimeline in rawTimelines) {
				val constraintName = ObjectDataParser._getString(rawTimeline, DataParser.NAME, "")
				val constraint = this._armature.getConstraint(constraintName)
				if (constraint === null) {
					continue
				}

				val timeline = this._parseTimeline(
					rawTimeline, null, DataParser.FRAME, TimelineType.IKConstraint,
					FrameValueType.Int, 2,
					this._parseIKConstraintFrame
				)

				if (timeline !== null) {
					this._animation.addConstraintTimeline(constraintName, timeline)
				}
			}
		}

		if (this._actionFrames.length > 0) {
			this._animation.actionTimeline = this._parseTimeline(
				null, this._actionFrames, "", TimelineType.Action,
				FrameValueType.Step, 0,
				this._parseActionFrame
			)
			this._actionFrames.length = 0
		}

		if (DataParser.TIMELINE in rawData) {
			val rawTimelines = rawData[DataParser.TIMELINE]
			for (rawTimeline in rawTimelines) {
				val timelineType = ObjectDataParser._getNumber(rawTimeline, DataParser.TYPE, TimelineType.Action) as TimelineType
				val timelineName = ObjectDataParser._getString(rawTimeline, DataParser.NAME, "")
				var timeline: TimelineData? = null

				switch (timelineType) {
					case TimelineType.Action:
						// TODO
						break

					case TimelineType.SlotDisplay: // TODO
					case TimelineType.SlotZIndex:
					case TimelineType.BoneAlpha:
					case TimelineType.SlotAlpha:
					case TimelineType.AnimationProgress:
					case TimelineType.AnimationWeight:
						if (
							timelineType === TimelineType.SlotDisplay
						) {
							this._frameValueType = FrameValueType.Step
							this._frameValueScale = 1.0
						}
						else {
							this._frameValueType = FrameValueType.Int

							if (timelineType === TimelineType.SlotZIndex) {
								this._frameValueScale = 1.0
							}
							else if (
								timelineType === TimelineType.AnimationProgress ||
								timelineType === TimelineType.AnimationWeight
							) {
								this._frameValueScale = 10000.0
							}
							else {
								this._frameValueScale = 100.0
							}
						}

						if (
							timelineType === TimelineType.BoneAlpha ||
							timelineType === TimelineType.SlotAlpha ||
							timelineType === TimelineType.AnimationWeight
						) {
							this._frameDefaultValue = 1.0
						}
						else {
							this._frameDefaultValue = 0.0
						}

						if (timelineType === TimelineType.AnimationProgress && animation.blendType !== AnimationBlendType.None) {
							timeline = BaseObject.borrowObject<AnimationTimelineData>()
							val animaitonTimeline = timeline as AnimationTimelineData
							animaitonTimeline.x = ObjectDataParser._getNumber(rawTimeline, DataParser.X, 0.0)
							animaitonTimeline.y = ObjectDataParser._getNumber(rawTimeline, DataParser.Y, 0.0)
						}

						timeline = this._parseTimeline(
							rawTimeline, null, DataParser.FRAME, timelineType,
							this._frameValueType, 1,
							this._parseSingleValueFrame, timeline
						)
					break

					case TimelineType.BoneTranslate:
					case TimelineType.BoneRotate:
					case TimelineType.BoneScale:
					case TimelineType.IKConstraint:
					case TimelineType.AnimationParameter:
						if (
							timelineType === TimelineType.IKConstraint ||
							timelineType === TimelineType.AnimationParameter
						) {
							this._frameValueType = FrameValueType.Int

							if (timelineType === TimelineType.AnimationParameter) {
								this._frameValueScale = 10000.0
							}
							else {
								this._frameValueScale = 100.0
							}
						}
						else {
							if (timelineType === TimelineType.BoneRotate) {
								this._frameValueScale = Transform.DEG_RAD
							}
							else {
								this._frameValueScale = 1.0
							}

							this._frameValueType = FrameValueType.Float
						}

						if (
							timelineType === TimelineType.BoneScale ||
							timelineType === TimelineType.IKConstraint
						) {
							this._frameDefaultValue = 1.0
						}
						else {
							this._frameDefaultValue = 0.0
						}

						timeline = this._parseTimeline(
							rawTimeline, null, DataParser.FRAME, timelineType,
							this._frameValueType, 2,
							this._parseDoubleValueFrame
						)
					break

					case TimelineType.ZOrder:
						// TODO
						break

					case TimelineType.Surface: {
						val surface = this._armature.getBone(timelineName) as SurfaceData
						if (surface === null) {
							continue
						}

						this._geometry = surface.geometry
						timeline = this._parseTimeline(
							rawTimeline, null, DataParser.FRAME, timelineType,
							FrameValueType.Float, 0,
							this._parseDeformFrame
						)

						this._geometry = null as any //
						break
					}

					case TimelineType.SlotDeform: {
						this._geometry = null as any //
						for (val skinName in this._armature.skins) {
							val skin = this._armature.skins[skinName]
							for (val slontName in skin.displays) {
								val displays = skin.displays[slontName]
								for (display in displays) {
									if (display !== null && display.name === timelineName) {
										this._geometry = (display as MeshDisplayData).geometry
										break
									}
								}
							}
						}

						if (this._geometry === null) {
							continue
						}

						timeline = this._parseTimeline(
							rawTimeline, null, DataParser.FRAME, timelineType,
							FrameValueType.Float, 0,
							this._parseDeformFrame
						)

						this._geometry = null as any //
						break
					}

					case TimelineType.SlotColor:
						timeline = this._parseTimeline(
							rawTimeline, null, DataParser.FRAME, timelineType,
							FrameValueType.Int, 1,
							this._parseSlotColorFrame
						)
					break
				}

				if (timeline !== null) {
					switch (timelineType) {
						case TimelineType.Action:
							// TODO
							break

						case TimelineType.ZOrder:
							// TODO
							break

						case TimelineType.BoneTranslate:
						case TimelineType.BoneRotate:
						case TimelineType.BoneScale:
						case TimelineType.Surface:
						case TimelineType.BoneAlpha:
							this._animation.addBoneTimeline(timelineName, timeline)
						break

						case TimelineType.SlotDisplay:
						case TimelineType.SlotColor:
						case TimelineType.SlotDeform:
						case TimelineType.SlotZIndex:
						case TimelineType.SlotAlpha:
							this._animation.addSlotTimeline(timelineName, timeline)
						break

						case TimelineType.IKConstraint:
							this._animation.addConstraintTimeline(timelineName, timeline)
						break

						case TimelineType.AnimationProgress:
						case TimelineType.AnimationWeight:
						case TimelineType.AnimationParameter:
							this._animation.addAnimationTimeline(timelineName, timeline)
						break
					}
				}
			}
		}

		this._animation = null as any //

		return animation
	}

	protected _parseTimeline(
		rawData: Any, rawFrames: Array<any>?, framesKey: String,
		timelineType: TimelineType, frameValueType: FrameValueType, frameValueCount: Double,
		frameParser: (rawData: Any, frameStart: Double, frameCount: Double) => number, timeline: TimelineData? = null
	): TimelineData? {
		if (rawData !== null && framesKey.length > 0 && framesKey in rawData) {
			rawFrames = rawData[framesKey]
		}

		if (rawFrames === null) {
			return null
		}

		val keyFrameCount = rawFrames.length
		if (keyFrameCount === 0) {
			return null
		}

		val frameIntArrayLength = this._frameIntArray.length
		val frameFloatArrayLength = this._frameFloatArray.length
		val timelineOffset = this._timelineArray.length
		if (timeline === null) {
			timeline = BaseObject.borrowObject<TimelineData>()
		}

		timeline.type = timelineType
		timeline.offset = timelineOffset
		this._frameValueType = frameValueType
		this._timeline = timeline
		this._timelineArray.length += 1 + 1 + 1 + 1 + 1 + keyFrameCount

		if (rawData !== null) {
			this._timelineArray[timelineOffset + BinaryOffset.TimelineScale] = Math.round(ObjectDataParser._getNumber(rawData, DataParser.SCALE, 1.0) * 100)
			this._timelineArray[timelineOffset + BinaryOffset.TimelineOffset] = Math.round(ObjectDataParser._getNumber(rawData, DataParser.OFFSET, 0.0) * 100)
		}
		else {
			this._timelineArray[timelineOffset + BinaryOffset.TimelineScale] = 100
			this._timelineArray[timelineOffset + BinaryOffset.TimelineOffset] = 0
		}

		this._timelineArray[timelineOffset + BinaryOffset.TimelineKeyFrameCount] = keyFrameCount
		this._timelineArray[timelineOffset + BinaryOffset.TimelineFrameValueCount] = frameValueCount

		switch (this._frameValueType) {
			case FrameValueType.Step:
				this._timelineArray[timelineOffset + BinaryOffset.TimelineFrameValueOffset] = 0
			break

			case FrameValueType.Int:
				this._timelineArray[timelineOffset + BinaryOffset.TimelineFrameValueOffset] = frameIntArrayLength - this._animation.frameIntOffset
			break

			case FrameValueType.Float:
				this._timelineArray[timelineOffset + BinaryOffset.TimelineFrameValueOffset] = frameFloatArrayLength - this._animation.frameFloatOffset
			break
		}

		if (keyFrameCount === 1) { // Only one frame.
			timeline.frameIndicesOffset = -1
			this._timelineArray[timelineOffset + BinaryOffset.TimelineFrameOffset + 0] = frameParser.call(this, rawFrames[0], 0, 0) - this._animation.frameOffset
		}
		else {
			val totalFrameCount = this._animation.frameCount + 1 // One more frame than animation.
			val frameIndices = this._data.frameIndices
			val frameIndicesOffset = frameIndices.length
			frameIndices.length += totalFrameCount
			timeline.frameIndicesOffset = frameIndicesOffset

			for (
				var i = 0, iK = 0, frameStart = 0, frameCount = 0
			i < totalFrameCount
			++i
			) {
				if (frameStart + frameCount <= i && iK < keyFrameCount) {
					val rawFrame = rawFrames[iK]
					frameStart = i // frame.frameStart;

					if (iK === keyFrameCount - 1) {
						frameCount = this._animation.frameCount - frameStart
					}
					else {
						if (rawFrame instanceof ActionFrame) {
							frameCount = this._actionFrames[iK + 1].frameStart - frameStart
						}
						else {
							frameCount = ObjectDataParser._getNumber(rawFrame, DataParser.DURATION, 1)
						}
					}

					this._timelineArray[timelineOffset + BinaryOffset.TimelineFrameOffset + iK] = frameParser.call(this, rawFrame, frameStart, frameCount) - this._animation.frameOffset
					iK++
				}

				frameIndices[frameIndicesOffset + i] = iK - 1
			}
		}

		this._timeline = null as any //

		return timeline
	}

	protected _parseBoneTimeline(rawData: Any): Unit {
		val bone = this._armature.getBone(ObjectDataParser._getString(rawData, DataParser.NAME, ""))
		if (bone === null) {
			return
		}

		this._bone = bone
		this._slot = this._armature.getSlot(this._bone.name) as any

		if (DataParser.TRANSLATE_FRAME in rawData) {
			this._frameDefaultValue = 0.0
			this._frameValueScale = 1.0
			val timeline = this._parseTimeline(
				rawData, null, DataParser.TRANSLATE_FRAME, TimelineType.BoneTranslate,
				FrameValueType.Float, 2,
				this._parseDoubleValueFrame
			)

			if (timeline !== null) {
				this._animation.addBoneTimeline(bone.name, timeline)
			}
		}

		if (DataParser.ROTATE_FRAME in rawData) {
			this._frameDefaultValue = 0.0
			this._frameValueScale = 1.0
			val timeline = this._parseTimeline(
				rawData, null, DataParser.ROTATE_FRAME, TimelineType.BoneRotate,
				FrameValueType.Float, 2,
				this._parseBoneRotateFrame
			)

			if (timeline !== null) {
				this._animation.addBoneTimeline(bone.name, timeline)
			}
		}

		if (DataParser.SCALE_FRAME in rawData) {
			this._frameDefaultValue = 1.0
			this._frameValueScale = 1.0
			val timeline = this._parseTimeline(
				rawData, null, DataParser.SCALE_FRAME, TimelineType.BoneScale,
				FrameValueType.Float, 2,
				this._parseBoneScaleFrame
			)

			if (timeline !== null) {
				this._animation.addBoneTimeline(bone.name, timeline)
			}
		}

		if (DataParser.FRAME in rawData) {
			val timeline = this._parseTimeline(
				rawData, null, DataParser.FRAME, TimelineType.BoneAll,
				FrameValueType.Float, 6,
				this._parseBoneAllFrame
			)

			if (timeline !== null) {
				this._animation.addBoneTimeline(bone.name, timeline)
			}
		}

		this._bone = null as any //
		this._slot = null as any //
	}

	protected _parseSlotTimeline(rawData: Any): Unit {
		val slot = this._armature.getSlot(ObjectDataParser._getString(rawData, DataParser.NAME, ""))
		if (slot === null) {
			return
		}

		var displayTimeline: TimelineData? = null
		var colorTimeline: TimelineData? = null
		this._slot = slot

		if (DataParser.DISPLAY_FRAME in rawData) {
			displayTimeline = this._parseTimeline(
				rawData, null, DataParser.DISPLAY_FRAME, TimelineType.SlotDisplay,
				FrameValueType.Step, 0,
				this._parseSlotDisplayFrame
			)
		}
		else {
			displayTimeline = this._parseTimeline(
				rawData, null, DataParser.FRAME, TimelineType.SlotDisplay,
				FrameValueType.Step, 0,
				this._parseSlotDisplayFrame
			)
		}

		if (DataParser.COLOR_FRAME in rawData) {
			colorTimeline = this._parseTimeline(
				rawData, null, DataParser.COLOR_FRAME, TimelineType.SlotColor,
				FrameValueType.Int, 1,
				this._parseSlotColorFrame
			)
		}
		else {
			colorTimeline = this._parseTimeline(
				rawData, null, DataParser.FRAME, TimelineType.SlotColor,
				FrameValueType.Int, 1,
				this._parseSlotColorFrame
			)
		}

		if (displayTimeline !== null) {
			this._animation.addSlotTimeline(slot.name, displayTimeline)
		}

		if (colorTimeline !== null) {
			this._animation.addSlotTimeline(slot.name, colorTimeline)
		}

		this._slot = null as any //
	}

	protected _parseFrame(rawData: Any, frameStart: Double, frameCount: Double): Double {
		// tslint:disable-next-line:no-unused-expression
		rawData
		// tslint:disable-next-line:no-unused-expression
		frameCount

		val frameOffset = this._frameArray.length
		this._frameArray.length += 1
		this._frameArray[frameOffset + BinaryOffset.FramePosition] = frameStart

		return frameOffset
	}

	protected _parseTweenFrame(rawData: Any, frameStart: Double, frameCount: Double): Double {
		val frameOffset = this._parseFrame(rawData, frameStart, frameCount)

		if (frameCount > 0) {
			if (DataParser.CURVE in rawData) {
				val sampleCount = frameCount + 1
				this._helpArray.length = sampleCount
				val isOmited = this._samplingEasingCurve(rawData[DataParser.CURVE], this._helpArray)

				this._frameArray.length += 1 + 1 + this._helpArray.length
				this._frameArray[frameOffset + BinaryOffset.FrameTweenType] = TweenType.Curve
				this._frameArray[frameOffset + BinaryOffset.FrameTweenEasingOrCurveSampleCount] = isOmited ? sampleCount : -sampleCount
				for (var i = 0; i < sampleCount; ++i) {
					this._frameArray[frameOffset + BinaryOffset.FrameCurveSamples + i] = Math.round(this._helpArray[i] * 10000.0)
				}
			}
			else {
				val noTween = -2.0
				var tweenEasing = noTween
				if (DataParser.TWEEN_EASING in rawData) {
					tweenEasing = ObjectDataParser._getNumber(rawData, DataParser.TWEEN_EASING, noTween)
				}

				if (tweenEasing === noTween) {
					this._frameArray.length += 1
					this._frameArray[frameOffset + BinaryOffset.FrameTweenType] = TweenType.None
				}
				else if (tweenEasing === 0.0) {
					this._frameArray.length += 1
					this._frameArray[frameOffset + BinaryOffset.FrameTweenType] = TweenType.Line
				}
				else if (tweenEasing < 0.0) {
					this._frameArray.length += 1 + 1
					this._frameArray[frameOffset + BinaryOffset.FrameTweenType] = TweenType.QuadIn
					this._frameArray[frameOffset + BinaryOffset.FrameTweenEasingOrCurveSampleCount] = Math.round(-tweenEasing * 100.0)
				}
				else if (tweenEasing <= 1.0) {
					this._frameArray.length += 1 + 1
					this._frameArray[frameOffset + BinaryOffset.FrameTweenType] = TweenType.QuadOut
					this._frameArray[frameOffset + BinaryOffset.FrameTweenEasingOrCurveSampleCount] = Math.round(tweenEasing * 100.0)
				}
				else {
					this._frameArray.length += 1 + 1
					this._frameArray[frameOffset + BinaryOffset.FrameTweenType] = TweenType.QuadInOut
					this._frameArray[frameOffset + BinaryOffset.FrameTweenEasingOrCurveSampleCount] = Math.round(tweenEasing * 100.0 - 100.0)
				}
			}
		}
		else {
			this._frameArray.length += 1
			this._frameArray[frameOffset + BinaryOffset.FrameTweenType] = TweenType.None
		}

		return frameOffset
	}

	protected _parseSingleValueFrame(rawData: Any, frameStart: Double, frameCount: Double): Double {
		var frameOffset = 0
		switch (this._frameValueType) {
			case 0: {
				frameOffset = this._parseFrame(rawData, frameStart, frameCount)
			this._frameArray.length += 1
			this._frameArray[frameOffset + 1] = ObjectDataParser._getNumber(rawData, DataParser.VALUE, this._frameDefaultValue)
			break
		}

			case 1: {
				frameOffset = this._parseTweenFrame(rawData, frameStart, frameCount)
			val frameValueOffset = this._frameIntArray.length
			this._frameIntArray.length += 1
			this._frameIntArray[frameValueOffset] = Math.round(ObjectDataParser._getNumber(rawData, DataParser.VALUE, this._frameDefaultValue) * this._frameValueScale)
			break
		}

			case 2: {
				frameOffset = this._parseTweenFrame(rawData, frameStart, frameCount)
			val frameValueOffset = this._frameFloatArray.length
			this._frameFloatArray.length += 1
			this._frameFloatArray[frameValueOffset] = ObjectDataParser._getNumber(rawData, DataParser.VALUE, this._frameDefaultValue) * this._frameValueScale
			break
		}
		}

		return frameOffset
	}

	protected _parseDoubleValueFrame(rawData: Any, frameStart: Double, frameCount: Double): Double {
		var frameOffset = 0
		switch (this._frameValueType) {
			case 0: {
				frameOffset = this._parseFrame(rawData, frameStart, frameCount)
			this._frameArray.length += 2
			this._frameArray[frameOffset + 1] = ObjectDataParser._getNumber(rawData, DataParser.X, this._frameDefaultValue)
			this._frameArray[frameOffset + 2] = ObjectDataParser._getNumber(rawData, DataParser.Y, this._frameDefaultValue)
			break
		}

			case 1: {
				frameOffset = this._parseTweenFrame(rawData, frameStart, frameCount)
			val frameValueOffset = this._frameIntArray.length
			this._frameIntArray.length += 2
			this._frameIntArray[frameValueOffset] = Math.round(ObjectDataParser._getNumber(rawData, DataParser.X, this._frameDefaultValue) * this._frameValueScale)
			this._frameIntArray[frameValueOffset + 1] = Math.round(ObjectDataParser._getNumber(rawData, DataParser.Y, this._frameDefaultValue) * this._frameValueScale)
			break
		}

			case 2: {
				frameOffset = this._parseTweenFrame(rawData, frameStart, frameCount)
			val frameValueOffset = this._frameFloatArray.length
			this._frameFloatArray.length += 2
			this._frameFloatArray[frameValueOffset] = ObjectDataParser._getNumber(rawData, DataParser.X, this._frameDefaultValue) * this._frameValueScale
			this._frameFloatArray[frameValueOffset + 1] = ObjectDataParser._getNumber(rawData, DataParser.Y, this._frameDefaultValue) * this._frameValueScale
			break
		}
		}

		return frameOffset
	}

	protected _parseActionFrame(frame: ActionFrame, frameStart: Double, frameCount: Double): Double {
		// tslint:disable-next-line:no-unused-expression
		frameCount

		val frameOffset = this._frameArray.length
		val actionCount = frame.actions.length
		this._frameArray.length += 1 + 1 + actionCount
		this._frameArray[frameOffset + BinaryOffset.FramePosition] = frameStart
		this._frameArray[frameOffset + BinaryOffset.FramePosition + 1] = actionCount // Action count.

		for (var i = 0; i < actionCount; ++i) { // Action offsets.
			this._frameArray[frameOffset + BinaryOffset.FramePosition + 2 + i] = frame.actions[i]
		}

		return frameOffset
	}

	protected _parseZOrderFrame(rawData: Any, frameStart: Double, frameCount: Double): Double {
		val frameOffset = this._parseFrame(rawData, frameStart, frameCount)

		if (DataParser.Z_ORDER in rawData) {
			val rawZOrder = rawData[DataParser.Z_ORDER] as  DoubleArray
			if (rawZOrder.length > 0) {
				val slotCount = this._armature.sortedSlots.length
				val unchanged = new  DoubleArray(slotCount - rawZOrder.length / 2)
				val zOrders = new  DoubleArray(slotCount)

				for (var i = 0; i < unchanged.length; ++i) {
					unchanged[i] = 0
				}

				for (var i = 0; i < slotCount; ++i) {
					zOrders[i] = -1
				}

				var originalIndex = 0
				var unchangedIndex = 0
				for (var i = 0, l = rawZOrder.length; i < l; i += 2) {
					val slotIndex = rawZOrder[i]
					val zOrderOffset = rawZOrder[i + 1]

					while (originalIndex !== slotIndex) {
						unchanged[unchangedIndex++] = originalIndex++
					}

					val index = originalIndex + zOrderOffset
					zOrders[index] = originalIndex++
				}

				while (originalIndex < slotCount) {
					unchanged[unchangedIndex++] = originalIndex++
				}

				this._frameArray.length += 1 + slotCount
				this._frameArray[frameOffset + 1] = slotCount

				var i = slotCount
				while (i--) {
					if (zOrders[i] === -1) {
						this._frameArray[frameOffset + 2 + i] = unchanged[--unchangedIndex] || 0
					}
					else {
						this._frameArray[frameOffset + 2 + i] = zOrders[i] || 0
					}
				}

				return frameOffset
			}
		}

		this._frameArray.length += 1
		this._frameArray[frameOffset + 1] = 0

		return frameOffset
	}

	protected _parseBoneAllFrame(rawData: Any, frameStart: Double, frameCount: Double): Double {
		this._helpTransform.identity()
		if (DataParser.TRANSFORM in rawData) {
			this._parseTransform(rawData[DataParser.TRANSFORM], this._helpTransform, 1.0)
		}

		// Modify rotation.
		var rotation = this._helpTransform.rotation
		if (frameStart !== 0) {
			if (this._prevClockwise === 0) {
				rotation = this._prevRotation + Transform.normalizeRadian(rotation - this._prevRotation)
			}
			else {
				if (this._prevClockwise > 0 ? rotation >= this._prevRotation : rotation <= this._prevRotation) {
					this._prevClockwise = this._prevClockwise > 0 ? this._prevClockwise - 1 : this._prevClockwise + 1
				}

				rotation = this._prevRotation + rotation - this._prevRotation + Transform.PI_D * this._prevClockwise
			}
		}

		this._prevClockwise = ObjectDataParser._getNumber(rawData, DataParser.TWEEN_ROTATE, 0.0)
		this._prevRotation = rotation
		//
		val frameOffset = this._parseTweenFrame(rawData, frameStart, frameCount)
		var frameFloatOffset = this._frameFloatArray.length
		this._frameFloatArray.length += 6
		this._frameFloatArray[frameFloatOffset++] = this._helpTransform.x
		this._frameFloatArray[frameFloatOffset++] = this._helpTransform.y
		this._frameFloatArray[frameFloatOffset++] = rotation
		this._frameFloatArray[frameFloatOffset++] = this._helpTransform.skew
		this._frameFloatArray[frameFloatOffset++] = this._helpTransform.scaleX
		this._frameFloatArray[frameFloatOffset++] = this._helpTransform.scaleY
		this._parseActionDataInFrame(rawData, frameStart, this._bone, this._slot)

		return frameOffset
	}

	protected _parseBoneTranslateFrame(rawData: Any, frameStart: Double, frameCount: Double): Double {
		val frameOffset = this._parseTweenFrame(rawData, frameStart, frameCount)
		var frameFloatOffset = this._frameFloatArray.length
		this._frameFloatArray.length += 2
		this._frameFloatArray[frameFloatOffset++] = ObjectDataParser._getNumber(rawData, DataParser.X, 0.0)
		this._frameFloatArray[frameFloatOffset++] = ObjectDataParser._getNumber(rawData, DataParser.Y, 0.0)

		return frameOffset
	}

	protected _parseBoneRotateFrame(rawData: Any, frameStart: Double, frameCount: Double): Double {
		// Modify rotation.
		var rotation = ObjectDataParser._getNumber(rawData, DataParser.ROTATE, 0.0) * Transform.DEG_RAD

		if (frameStart !== 0) {
			if (this._prevClockwise === 0) {
				rotation = this._prevRotation + Transform.normalizeRadian(rotation - this._prevRotation)
			}
			else {
				if (this._prevClockwise > 0 ? rotation >= this._prevRotation : rotation <= this._prevRotation) {
					this._prevClockwise = this._prevClockwise > 0 ? this._prevClockwise - 1 : this._prevClockwise + 1
				}

				rotation = this._prevRotation + rotation - this._prevRotation + Transform.PI_D * this._prevClockwise
			}
		}

		this._prevClockwise = ObjectDataParser._getNumber(rawData, DataParser.CLOCK_WISE, 0)
		this._prevRotation = rotation
		//
		val frameOffset = this._parseTweenFrame(rawData, frameStart, frameCount)
		var frameFloatOffset = this._frameFloatArray.length
		this._frameFloatArray.length += 2
		this._frameFloatArray[frameFloatOffset++] = rotation
		this._frameFloatArray[frameFloatOffset++] = ObjectDataParser._getNumber(rawData, DataParser.SKEW, 0.0) * Transform.DEG_RAD

		return frameOffset
	}

	protected _parseBoneScaleFrame(rawData: Any, frameStart: Double, frameCount: Double): Double {
		val frameOffset = this._parseTweenFrame(rawData, frameStart, frameCount)
		var frameFloatOffset = this._frameFloatArray.length
		this._frameFloatArray.length += 2
		this._frameFloatArray[frameFloatOffset++] = ObjectDataParser._getNumber(rawData, DataParser.X, 1.0)
		this._frameFloatArray[frameFloatOffset++] = ObjectDataParser._getNumber(rawData, DataParser.Y, 1.0)

		return frameOffset
	}

	protected _parseSlotDisplayFrame(rawData: Any, frameStart: Double, frameCount: Double): Double {
		val frameOffset = this._parseFrame(rawData, frameStart, frameCount)
		this._frameArray.length += 1

		if (DataParser.VALUE in rawData) {
			this._frameArray[frameOffset + 1] = ObjectDataParser._getNumber(rawData, DataParser.VALUE, 0)
		}
		else {
			this._frameArray[frameOffset + 1] = ObjectDataParser._getNumber(rawData, DataParser.DISPLAY_INDEX, 0)
		}

		this._parseActionDataInFrame(rawData, frameStart, this._slot.parent, this._slot)

		return frameOffset
	}

	protected _parseSlotColorFrame(rawData: Any, frameStart: Double, frameCount: Double): Double {
		val frameOffset = this._parseTweenFrame(rawData, frameStart, frameCount)
		var colorOffset = -1

		if (DataParser.VALUE in rawData || DataParser.COLOR in rawData) {
			val rawColor = DataParser.VALUE in rawData ? rawData[DataParser.VALUE] : rawData[DataParser.COLOR]
			for (var k in rawColor) { // Detects the presence of color.
				// tslint:disable-next-line:no-unused-expression
				k
				this._parseColorTransform(rawColor, this._helpColorTransform)
				colorOffset = this._colorArray.length
				this._colorArray.length += 8
				this._colorArray[colorOffset++] = Math.round(this._helpColorTransform.alphaMultiplier * 100)
				this._colorArray[colorOffset++] = Math.round(this._helpColorTransform.redMultiplier * 100)
				this._colorArray[colorOffset++] = Math.round(this._helpColorTransform.greenMultiplier * 100)
				this._colorArray[colorOffset++] = Math.round(this._helpColorTransform.blueMultiplier * 100)
				this._colorArray[colorOffset++] = Math.round(this._helpColorTransform.alphaOffset)
				this._colorArray[colorOffset++] = Math.round(this._helpColorTransform.redOffset)
				this._colorArray[colorOffset++] = Math.round(this._helpColorTransform.greenOffset)
				this._colorArray[colorOffset++] = Math.round(this._helpColorTransform.blueOffset)
				colorOffset -= 8
				break
			}
		}

		if (colorOffset < 0) {
			if (this._defaultColorOffset < 0) {
				this._defaultColorOffset = colorOffset = this._colorArray.length
				this._colorArray.length += 8
				this._colorArray[colorOffset++] = 100
				this._colorArray[colorOffset++] = 100
				this._colorArray[colorOffset++] = 100
				this._colorArray[colorOffset++] = 100
				this._colorArray[colorOffset++] = 0
				this._colorArray[colorOffset++] = 0
				this._colorArray[colorOffset++] = 0
				this._colorArray[colorOffset++] = 0
			}

			colorOffset = this._defaultColorOffset
		}

		val frameIntOffset = this._frameIntArray.length
		this._frameIntArray.length += 1
		this._frameIntArray[frameIntOffset] = colorOffset

		return frameOffset
	}

	protected _parseSlotDeformFrame(rawData: Any, frameStart: Double, frameCount: Double): Double {
		val frameFloatOffset = this._frameFloatArray.length
		val frameOffset = this._parseTweenFrame(rawData, frameStart, frameCount)
		val rawVertices = DataParser.VERTICES in rawData ? rawData[DataParser.VERTICES] as  DoubleArray : null
		val offset = ObjectDataParser._getNumber(rawData, DataParser.OFFSET, 0) // uint
		val vertexCount = this._intArray[this._mesh.geometry.offset + BinaryOffset.GeometryVertexCount]
		val meshName = this._mesh.parent.name + "_" + this._slot.name + "_" + this._mesh.name
		val weight = this._mesh.geometry.weight

		var x = 0.0
		var y = 0.0
		var iB = 0
		var iV = 0
		if (weight !== null) {
			val rawSlotPose = this._weightSlotPose[meshName]
			this._helpMatrixA.copyFromArray(rawSlotPose, 0)
			this._frameFloatArray.length += weight.count * 2
			iB = weight.offset + BinaryOffset.WeigthBoneIndices + weight.bones.length
		}
		else {
			this._frameFloatArray.length += vertexCount * 2
		}

		for (
			var i = 0
		i < vertexCount * 2
		i += 2
		) {
			if (rawVertices === null) { // Fill 0.
				x = 0.0
				y = 0.0
			}
			else {
				if (i < offset || i - offset >= rawVertices.length) {
					x = 0.0
				}
				else {
					x = rawVertices[i - offset]
				}

				if (i + 1 < offset || i + 1 - offset >= rawVertices.length) {
					y = 0.0
				}
				else {
					y = rawVertices[i + 1 - offset]
				}
			}

			if (weight !== null) { // If mesh is skinned, transform point by bone bind pose.
				val rawBonePoses = this._weightBonePoses[meshName]
				val vertexBoneCount = this._intArray[iB++]

				this._helpMatrixA.transformPoint(x, y, this._helpPoint, true)
				x = this._helpPoint.x
				y = this._helpPoint.y

				for (var j = 0; j < vertexBoneCount; ++j) {
					val boneIndex = this._intArray[iB++]
					this._helpMatrixB.copyFromArray(rawBonePoses, boneIndex * 7 + 1)
					this._helpMatrixB.invert()
					this._helpMatrixB.transformPoint(x, y, this._helpPoint, true)

					this._frameFloatArray[frameFloatOffset + iV++] = this._helpPoint.x
					this._frameFloatArray[frameFloatOffset + iV++] = this._helpPoint.y
				}
			}
			else {
				this._frameFloatArray[frameFloatOffset + i] = x
				this._frameFloatArray[frameFloatOffset + i + 1] = y
			}
		}

		if (frameStart === 0) {
			val frameIntOffset = this._frameIntArray.length
			this._frameIntArray.length += 1 + 1 + 1 + 1 + 1
			this._frameIntArray[frameIntOffset + BinaryOffset.DeformVertexOffset] = this._mesh.geometry.offset
			this._frameIntArray[frameIntOffset + BinaryOffset.DeformCount] = this._frameFloatArray.length - frameFloatOffset
			this._frameIntArray[frameIntOffset + BinaryOffset.DeformValueCount] = this._frameFloatArray.length - frameFloatOffset
			this._frameIntArray[frameIntOffset + BinaryOffset.DeformValueOffset] = 0
			this._frameIntArray[frameIntOffset + BinaryOffset.DeformFloatOffset] = frameFloatOffset - this._animation.frameFloatOffset
			this._timelineArray[this._timeline.offset + BinaryOffset.TimelineFrameValueCount] = frameIntOffset - this._animation.frameIntOffset
		}

		return frameOffset
	}

	protected _parseIKConstraintFrame(rawData: Any, frameStart: Double, frameCount: Double): Double {
		val frameOffset = this._parseTweenFrame(rawData, frameStart, frameCount)
		var frameIntOffset = this._frameIntArray.length
		this._frameIntArray.length += 2
		this._frameIntArray[frameIntOffset++] = ObjectDataParser._getBoolean(rawData, DataParser.BEND_POSITIVE, true) ? 1 : 0
		this._frameIntArray[frameIntOffset++] = Math.round(ObjectDataParser._getNumber(rawData, DataParser.WEIGHT, 1.0) * 100.0)

		return frameOffset
	}

	protected _parseActionData(rawData: Any, type: ActionType, bone: BoneData?, slot: SlotData?): Array<ActionData> {
		val actions = new Array<ActionData>()

		if (typeof rawData === "string") {
			val action = BaseObject.borrowObject<ActionData>()
			action.type = type
			action.name = rawData
			action.bone = bone
			action.slot = slot
			actions.push(action)
		}
		else if (rawData instanceof Array) {
			for (rawAction in rawData) {
				val action = BaseObject.borrowObject<ActionData>()

				if (DataParser.GOTO_AND_PLAY in rawAction) {
					action.type = ActionType.Play
					action.name = ObjectDataParser._getString(rawAction, DataParser.GOTO_AND_PLAY, "")
				}
				else {
					if (DataParser.TYPE in rawAction && typeof rawAction[DataParser.TYPE] === "string") {
						action.type = DataParser._getActionType(rawAction[DataParser.TYPE])
					}
					else {
						action.type = ObjectDataParser._getNumber(rawAction, DataParser.TYPE, type)
					}

					action.name = ObjectDataParser._getString(rawAction, DataParser.NAME, "")
				}

				if (DataParser.BONE in rawAction) {
					val boneName = ObjectDataParser._getString(rawAction, DataParser.BONE, "")
					action.bone = this._armature.getBone(boneName)
				}
				else {
					action.bone = bone
				}

				if (DataParser.SLOT in rawAction) {
					val slotName = ObjectDataParser._getString(rawAction, DataParser.SLOT, "")
					action.slot = this._armature.getSlot(slotName)
				}
				else {
					action.slot = slot
				}

				var userData: UserData? = null

				if (DataParser.INTS in rawAction) {
					if (userData === null) {
						userData = BaseObject.borrowObject<UserData>()
					}

					val rawInts = rawAction[DataParser.INTS] as  DoubleArray
					for (rawValue in rawInts) {
						userData.addInt(rawValue)
					}
				}

				if (DataParser.FLOATS in rawAction) {
					if (userData === null) {
						userData = BaseObject.borrowObject<UserData>()
					}

					val rawFloats = rawAction[DataParser.FLOATS] as  DoubleArray
					for (rawValue in rawFloats) {
						userData.addFloat(rawValue)
					}
				}

				if (DataParser.STRINGS in rawAction) {
					if (userData === null) {
						userData = BaseObject.borrowObject<UserData>()
					}

					val rawStrings = rawAction[DataParser.STRINGS] as Array<string>
					for (rawValue in rawStrings) {
						userData.addString(rawValue)
					}
				}

				action.data = userData
				actions.push(action)
			}
		}

		return actions
	}

	protected _parseDeformFrame(rawData: Any, frameStart: Double, frameCount: Double): Double {
		val frameFloatOffset = this._frameFloatArray.length
		val frameOffset = this._parseTweenFrame(rawData, frameStart, frameCount)
		val rawVertices = DataParser.VERTICES in rawData ?
			rawData[DataParser.VERTICES] as  DoubleArray :
			(DataParser.VALUE in rawData ? rawData[DataParser.VALUE] as  DoubleArray : null)
		val offset = ObjectDataParser._getNumber(rawData, DataParser.OFFSET, 0) // uint
		val vertexCount = this._intArray[this._geometry.offset + BinaryOffset.GeometryVertexCount]
		val weight = this._geometry.weight
		var x = 0.0
		var y = 0.0

		if (weight !== null) {
			// TODO
		}
		else {
			this._frameFloatArray.length += vertexCount * 2

			for (
				var i = 0
			i < vertexCount * 2
			i += 2
			) {
				if (rawVertices !== null) {
					if (i < offset || i - offset >= rawVertices.length) {
						x = 0.0
					}
					else {
						x = rawVertices[i - offset]
					}

					if (i + 1 < offset || i + 1 - offset >= rawVertices.length) {
						y = 0.0
					}
					else {
						y = rawVertices[i + 1 - offset]
					}
				}
				else {
					x = 0.0
					y = 0.0
				}

				this._frameFloatArray[frameFloatOffset + i] = x
				this._frameFloatArray[frameFloatOffset + i + 1] = y
			}
		}

		if (frameStart === 0) {
			val frameIntOffset = this._frameIntArray.length
			this._frameIntArray.length += 1 + 1 + 1 + 1 + 1
			this._frameIntArray[frameIntOffset + BinaryOffset.DeformVertexOffset] = this._geometry.offset
			this._frameIntArray[frameIntOffset + BinaryOffset.DeformCount] = this._frameFloatArray.length - frameFloatOffset
			this._frameIntArray[frameIntOffset + BinaryOffset.DeformValueCount] = this._frameFloatArray.length - frameFloatOffset
			this._frameIntArray[frameIntOffset + BinaryOffset.DeformValueOffset] = 0
			this._frameIntArray[frameIntOffset + BinaryOffset.DeformFloatOffset] = frameFloatOffset - this._animation.frameFloatOffset
			this._timelineArray[this._timeline.offset + BinaryOffset.TimelineFrameValueCount] = frameIntOffset - this._animation.frameIntOffset
		}

		return frameOffset
	}

	protected _parseTransform(rawData: Any, transform: Transform, scale: Double): Unit {
		transform.x = ObjectDataParser._getNumber(rawData, DataParser.X, 0.0) * scale
		transform.y = ObjectDataParser._getNumber(rawData, DataParser.Y, 0.0) * scale

		if (DataParser.ROTATE in rawData || DataParser.SKEW in rawData) {
			transform.rotation = Transform.normalizeRadian(ObjectDataParser._getNumber(rawData, DataParser.ROTATE, 0.0) * Transform.DEG_RAD)
			transform.skew = Transform.normalizeRadian(ObjectDataParser._getNumber(rawData, DataParser.SKEW, 0.0) * Transform.DEG_RAD)
		}
		else if (DataParser.SKEW_X in rawData || DataParser.SKEW_Y in rawData) {
			transform.rotation = Transform.normalizeRadian(ObjectDataParser._getNumber(rawData, DataParser.SKEW_Y, 0.0) * Transform.DEG_RAD)
			transform.skew = Transform.normalizeRadian(ObjectDataParser._getNumber(rawData, DataParser.SKEW_X, 0.0) * Transform.DEG_RAD) - transform.rotation
		}

		transform.scaleX = ObjectDataParser._getNumber(rawData, DataParser.SCALE_X, 1.0)
		transform.scaleY = ObjectDataParser._getNumber(rawData, DataParser.SCALE_Y, 1.0)
	}

	protected _parseColorTransform(rawData: Any, color: ColorTransform): Unit {
		color.alphaMultiplier = ObjectDataParser._getNumber(rawData, DataParser.ALPHA_MULTIPLIER, 100) * 0.01
		color.redMultiplier = ObjectDataParser._getNumber(rawData, DataParser.RED_MULTIPLIER, 100) * 0.01
		color.greenMultiplier = ObjectDataParser._getNumber(rawData, DataParser.GREEN_MULTIPLIER, 100) * 0.01
		color.blueMultiplier = ObjectDataParser._getNumber(rawData, DataParser.BLUE_MULTIPLIER, 100) * 0.01
		color.alphaOffset = ObjectDataParser._getNumber(rawData, DataParser.ALPHA_OFFSET, 0)
		color.redOffset = ObjectDataParser._getNumber(rawData, DataParser.RED_OFFSET, 0)
		color.greenOffset = ObjectDataParser._getNumber(rawData, DataParser.GREEN_OFFSET, 0)
		color.blueOffset = ObjectDataParser._getNumber(rawData, DataParser.BLUE_OFFSET, 0)
	}

	protected _parseGeometry(rawData: Any, geometry: GeometryData): Unit {
		val rawVertices = rawData[DataParser.VERTICES] as  DoubleArray
		val vertexCount = Math.floor(rawVertices.length / 2) // uint
		var triangleCount = 0
		val geometryOffset = this._intArray.length
		val verticesOffset = this._floatArray.length
		//
		geometry.offset = geometryOffset
		geometry.data = this._data
		//
		this._intArray.length += 1 + 1 + 1 + 1
		this._intArray[geometryOffset + BinaryOffset.GeometryVertexCount] = vertexCount
		this._intArray[geometryOffset + BinaryOffset.GeometryFloatOffset] = verticesOffset
		this._intArray[geometryOffset + BinaryOffset.GeometryWeightOffset] = -1 //
		//
		this._floatArray.length += vertexCount * 2
		for (var i = 0, l = vertexCount * 2; i < l; ++i) {
			this._floatArray[verticesOffset + i] = rawVertices[i]
		}

		if (DataParser.TRIANGLES in rawData) {
			val rawTriangles = rawData[DataParser.TRIANGLES] as  DoubleArray
			triangleCount = Math.floor(rawTriangles.length / 3) // uint
			//
			this._intArray.length += triangleCount * 3
			for (var i = 0, l = triangleCount * 3; i < l; ++i) {
				this._intArray[geometryOffset + BinaryOffset.GeometryVertexIndices + i] = rawTriangles[i]
			}
		}
		// Fill triangle count.
		this._intArray[geometryOffset + BinaryOffset.GeometryTriangleCount] = triangleCount

		if (DataParser.UVS in rawData) {
			val rawUVs = rawData[DataParser.UVS] as  DoubleArray
			val uvOffset = verticesOffset + vertexCount * 2
			this._floatArray.length += vertexCount * 2
			for (var i = 0, l = vertexCount * 2; i < l; ++i) {
				this._floatArray[uvOffset + i] = rawUVs[i]
			}
		}

		if (DataParser.WEIGHTS in rawData) {
			val rawWeights = rawData[DataParser.WEIGHTS] as  DoubleArray
			val weightCount = Math.floor(rawWeights.length - vertexCount) / 2 // uint
			val weightOffset = this._intArray.length
			val floatOffset = this._floatArray.length
			var weightBoneCount = 0
			val sortedBones = this._armature.sortedBones
			val weight = BaseObject.borrowObject<WeightData>()
			weight.count = weightCount
			weight.offset = weightOffset

			this._intArray.length += 1 + 1 + weightBoneCount + vertexCount + weightCount
			this._intArray[weightOffset + BinaryOffset.WeigthFloatOffset] = floatOffset

			if (DataParser.BONE_POSE in rawData) {
				val rawSlotPose = rawData[DataParser.SLOT_POSE] as  DoubleArray
				val rawBonePoses = rawData[DataParser.BONE_POSE] as  DoubleArray
				val weightBoneIndices = new  DoubleArray()

				weightBoneCount = Math.floor(rawBonePoses.length / 7) // uint
				weightBoneIndices.length = weightBoneCount

				for (var i = 0; i < weightBoneCount; ++i) {
					val rawBoneIndex = rawBonePoses[i * 7] // uint
					val bone = this._rawBones[rawBoneIndex]
					weight.addBone(bone)
					weightBoneIndices[i] = rawBoneIndex
					this._intArray[weightOffset + BinaryOffset.WeigthBoneIndices + i] = sortedBones.indexOf(bone)
				}

				this._floatArray.length += weightCount * 3
				this._helpMatrixA.copyFromArray(rawSlotPose, 0)

				for (
					var i = 0, iW = 0, iB = weightOffset + BinaryOffset.WeigthBoneIndices + weightBoneCount, iV = floatOffset
				i < vertexCount
				++i
				) {
					val iD = i * 2
					val vertexBoneCount = this._intArray[iB++] = rawWeights[iW++] // uint

					var x = this._floatArray[verticesOffset + iD]
					var y = this._floatArray[verticesOffset + iD + 1]
					this._helpMatrixA.transformPoint(x, y, this._helpPoint)
					x = this._helpPoint.x
					y = this._helpPoint.y

					for (var j = 0; j < vertexBoneCount; ++j) {
						val rawBoneIndex = rawWeights[iW++] // uint
						val boneIndex = weightBoneIndices.indexOf(rawBoneIndex)
					this._helpMatrixB.copyFromArray(rawBonePoses, boneIndex * 7 + 1)
					this._helpMatrixB.invert()
					this._helpMatrixB.transformPoint(x, y, this._helpPoint)
					this._intArray[iB++] = boneIndex
					this._floatArray[iV++] = rawWeights[iW++]
					this._floatArray[iV++] = this._helpPoint.x
					this._floatArray[iV++] = this._helpPoint.y
				}
				}
			}
			else {
				val rawBones = rawData[DataParser.BONES] as  DoubleArray
				weightBoneCount = rawBones.length

				for (var i = 0; i < weightBoneCount; i++) {
					val rawBoneIndex = rawBones[i]
					val bone = this._rawBones[rawBoneIndex]
					weight.addBone(bone)
					this._intArray[weightOffset + BinaryOffset.WeigthBoneIndices + i] = sortedBones.indexOf(bone)
				}

				this._floatArray.length += weightCount * 3
				for (var i = 0, iW = 0, iV = 0, iB = weightOffset + BinaryOffset.WeigthBoneIndices + weightBoneCount, iF = floatOffset
				i < weightCount
				i++
				) {
					val vertexBoneCount = rawWeights[iW++]
					this._intArray[iB++] = vertexBoneCount

					for (var j = 0; j < vertexBoneCount; j++) {
						val boneIndex = rawWeights[iW++]
					val boneWeight = rawWeights[iW++]
					val x = rawVertices[iV++]
					val y = rawVertices[iV++]

					this._intArray[iB++] = rawBones.indexOf(boneIndex)
					this._floatArray[iF++] = boneWeight
					this._floatArray[iF++] = x
					this._floatArray[iF++] = y
				}
				}
			}

			geometry.weight = weight
		}
	}

	protected _parseArray(rawData: Any): Unit {
		// tslint:disable-next-line:no-unused-expression
		rawData
		this._intArray.length = 0
		this._floatArray.length = 0
		this._frameIntArray.length = 0
		this._frameFloatArray.length = 0
		this._frameArray.length = 0
		this._timelineArray.length = 0
		this._colorArray.length = 0
	}

	protected _modifyArray(): Unit {
		// Align.
		if ((this._intArray.length % Int16Array.BYTES_PER_ELEMENT) !== 0) {
			this._intArray.push(0)
		}

		if ((this._frameIntArray.length % Int16Array.BYTES_PER_ELEMENT) !== 0) {
			this._frameIntArray.push(0)
		}

		if ((this._frameArray.length % Int16Array.BYTES_PER_ELEMENT) !== 0) {
			this._frameArray.push(0)
		}

		if ((this._timelineArray.length % Uint16Array.BYTES_PER_ELEMENT) !== 0) {
			this._timelineArray.push(0)
		}

		if ((this._timelineArray.length % Int16Array.BYTES_PER_ELEMENT) !== 0) {
			this._colorArray.push(0)
		}

		val l1 = this._intArray.length * Int16Array.BYTES_PER_ELEMENT
		val l2 = this._floatArray.length * Float32Array.BYTES_PER_ELEMENT
		val l3 = this._frameIntArray.length * Int16Array.BYTES_PER_ELEMENT
		val l4 = this._frameFloatArray.length * Float32Array.BYTES_PER_ELEMENT
		val l5 = this._frameArray.length * Int16Array.BYTES_PER_ELEMENT
		val l6 = this._timelineArray.length * Uint16Array.BYTES_PER_ELEMENT
		val l7 = this._colorArray.length * Int16Array.BYTES_PER_ELEMENT
		val lTotal = l1 + l2 + l3 + l4 + l5 + l6 + l7
		//
		val binary = new ArrayBuffer(lTotal)
		val intArray = new Int16Array(binary, 0, this._intArray.length)
		val floatArray = new Float32Array(binary, l1, this._floatArray.length)
		val frameIntArray = new Int16Array(binary, l1 + l2, this._frameIntArray.length)
		val frameFloatArray = new Float32Array(binary, l1 + l2 + l3, this._frameFloatArray.length)
		val frameArray = new Int16Array(binary, l1 + l2 + l3 + l4, this._frameArray.length)
		val timelineArray = new Uint16Array(binary, l1 + l2 + l3 + l4 + l5, this._timelineArray.length)
		val colorArray = new Int16Array(binary, l1 + l2 + l3 + l4 + l5 + l6, this._colorArray.length)

		for (var i = 0, l = this._intArray.length; i < l; ++i) {
			intArray[i] = this._intArray[i]
		}

		for (var i = 0, l = this._floatArray.length; i < l; ++i) {
			floatArray[i] = this._floatArray[i]
		}

		for (var i = 0, l = this._frameIntArray.length; i < l; ++i) {
			frameIntArray[i] = this._frameIntArray[i]
		}

		for (var i = 0, l = this._frameFloatArray.length; i < l; ++i) {
			frameFloatArray[i] = this._frameFloatArray[i]
		}

		for (var i = 0, l = this._frameArray.length; i < l; ++i) {
			frameArray[i] = this._frameArray[i]
		}

		for (var i = 0, l = this._timelineArray.length; i < l; ++i) {
			timelineArray[i] = this._timelineArray[i]
		}

		for (var i = 0, l = this._colorArray.length; i < l; ++i) {
			colorArray[i] = this._colorArray[i]
		}

		this._data.binary = binary
		this._data.intArray = intArray
		this._data.floatArray = floatArray
		this._data.frameIntArray = frameIntArray
		this._data.frameFloatArray = frameFloatArray
		this._data.frameArray = frameArray
		this._data.timelineArray = timelineArray
		this._data.colorArray = colorArray
		this._defaultColorOffset = -1
	}

	public parseDragonBonesData(rawData: Any, scale: Double = 1): DragonBonesData? {
		console.assert(rawData !== null && rawData !== undefined, "Data error.")

		val version = ObjectDataParser._getString(rawData, DataParser.VERSION, "")
		val compatibleVersion = ObjectDataParser._getString(rawData, DataParser.COMPATIBLE_VERSION, "")

		if (
			DataParser.DATA_VERSIONS.indexOf(version) >= 0 ||
			DataParser.DATA_VERSIONS.indexOf(compatibleVersion) >= 0
		) {
			val data = BaseObject.borrowObject<DragonBonesData>()
			data.version = version
			data.name = ObjectDataParser._getString(rawData, DataParser.NAME, "")
			data.frameRate = ObjectDataParser._getNumber(rawData, DataParser.FRAME_RATE, 24)

			if (data.frameRate === 0) { // Data error.
				data.frameRate = 24
			}

			if (DataParser.ARMATURE in rawData) {
				this._data = data
				this._parseArray(rawData)

				val rawArmatures = rawData[DataParser.ARMATURE] as Array<any>
				for (rawArmature in rawArmatures) {
					data.addArmature(this._parseArmature(rawArmature, scale))
				}

				if (!this._data.binary) { // DragonBones.webAssembly ? 0 : null;
					this._modifyArray()
				}

				if (DataParser.STAGE in rawData) {
					data.stage = data.getArmature(ObjectDataParser._getString(rawData, DataParser.STAGE, ""))
				}
				else if (data.armatureNames.length > 0) {
					data.stage = data.getArmature(data.armatureNames[0])
				}

				this._data = null as any
			}

			if (DataParser.TEXTURE_ATLAS in rawData) {
				this._rawTextureAtlases = rawData[DataParser.TEXTURE_ATLAS]
			}

			return data
		}
		else {
			console.assert(
				false,
				"Nonsupport data version: " + version + "\n" +
				"Please convert DragonBones data to support version.\n" +
				"Read more: https://github.com/DragonBones/Tools/"
			)
		}

		return null
	}

	public parseTextureAtlasData(rawData: Any, textureAtlasData: TextureAtlasData, scale: Double = 1.0): Boolean {
		console.assert(rawData !== undefined)

		if (rawData === null) {
			if (this._rawTextureAtlases === null || this._rawTextureAtlases.length === 0) {
				return false
			}

			val rawTextureAtlas = this._rawTextureAtlases[this._rawTextureAtlasIndex++]
			this.parseTextureAtlasData(rawTextureAtlas, textureAtlasData, scale)

			if (this._rawTextureAtlasIndex >= this._rawTextureAtlases.length) {
				this._rawTextureAtlasIndex = 0
				this._rawTextureAtlases = null
			}

			return true
		}

		// Texture format.
		textureAtlasData.width = ObjectDataParser._getNumber(rawData, DataParser.WIDTH, 0)
		textureAtlasData.height = ObjectDataParser._getNumber(rawData, DataParser.HEIGHT, 0)
		textureAtlasData.scale = scale === 1.0 ? (1.0 / ObjectDataParser._getNumber(rawData, DataParser.SCALE, 1.0)) : scale
		textureAtlasData.name = ObjectDataParser._getString(rawData, DataParser.NAME, "")
		textureAtlasData.imagePath = ObjectDataParser._getString(rawData, DataParser.IMAGE_PATH, "")

		if (DataParser.SUB_TEXTURE in rawData) {
			val rawTextures = rawData[DataParser.SUB_TEXTURE] as Array<any>
			for (var i = 0, l = rawTextures.length; i < l; ++i) {
				val rawTexture = rawTextures[i]
				val frameWidth = ObjectDataParser._getNumber(rawTexture, DataParser.FRAME_WIDTH, -1.0)
				val frameHeight = ObjectDataParser._getNumber(rawTexture, DataParser.FRAME_HEIGHT, -1.0)
				val textureData = textureAtlasData.createTexture()

				textureData.rotated = ObjectDataParser._getBoolean(rawTexture, DataParser.ROTATED, false)
				textureData.name = ObjectDataParser._getString(rawTexture, DataParser.NAME, "")
				textureData.region.x = ObjectDataParser._getNumber(rawTexture, DataParser.X, 0.0)
				textureData.region.y = ObjectDataParser._getNumber(rawTexture, DataParser.Y, 0.0)
				textureData.region.width = ObjectDataParser._getNumber(rawTexture, DataParser.WIDTH, 0.0)
				textureData.region.height = ObjectDataParser._getNumber(rawTexture, DataParser.HEIGHT, 0.0)

				if (frameWidth > 0.0 && frameHeight > 0.0) {
					textureData.frame = TextureData.createRectangle()
					textureData.frame.x = ObjectDataParser._getNumber(rawTexture, DataParser.FRAME_X, 0.0)
					textureData.frame.y = ObjectDataParser._getNumber(rawTexture, DataParser.FRAME_Y, 0.0)
					textureData.frame.width = frameWidth
					textureData.frame.height = frameHeight
				}

				textureAtlasData.addTexture(textureData)
			}
		}

		return true
	}
}
/**
 * @private
 */
class ActionFrame {
	public frameStart: Double = 0
	public val actions:  DoubleArray = []
}
