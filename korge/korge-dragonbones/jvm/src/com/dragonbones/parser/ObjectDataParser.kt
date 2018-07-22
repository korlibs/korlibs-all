package com.dragonbones.parser

import com.dragonbones.core.*
import com.dragonbones.factory.BaseFactory
import com.dragonbones.geom.ColorTransform
import com.dragonbones.geom.Matrix
import com.dragonbones.geom.Point
import com.dragonbones.geom.Transform
import com.dragonbones.model.*
import com.dragonbones.util.*

import java.util.*

import com.dragonbones.util.Dynamic.*

/**
 * @private
 */
open class ObjectDataParser : DataParser() {

    /**
     * @private
     */

    private val _intArrayJson = IntArray()
    private val _floatArrayJson = FloatArray()
    private val _frameIntArrayJson = IntArray()
    private val _frameFloatArrayJson = FloatArray()
    private val _frameArrayJson = FloatArray()
    private val _timelineArrayJson = FloatArray()

    private val _intArrayBuffer: ShortArray? = null
    private val _floatArrayBuffer: FloatArray? = null
    private val _frameIntArrayBuffer: ShortArray? = null
    private val _frameFloatArrayBuffer: FloatArray? = null
    private val _frameArrayBuffer: ShortArray? = null
    private val _timelineArrayBuffer: CharArray? = null

    protected var _rawTextureAtlasIndex = 0
    protected val _rawBones = Array<BoneData>()
    protected var _data: DragonBonesData? = null //
    protected var _armature: ArmatureData? = null //
    protected var _bone: BoneData? = null //
    protected var _slot: SlotData? = null //
    protected var _skin: SkinData? = null //
    protected var _mesh: MeshDisplayData? = null //
    protected var _animation: AnimationData? = null //
    protected var _timeline: TimelineData? = null //
    protected var _rawTextureAtlases: Array<Any>? = null

    private var _defalultColorOffset = -1
    private var _prevTweenRotate = 0f
    private var _prevRotation = 0f
    private val _helpMatrixA = Matrix()

    private val _helpMatrixB = Matrix()

    private val _helpTransform = Transform()

    private val _helpColorTransform = ColorTransform()

    private val _helpPoint = Point()

    private val _helpArray = FloatArray()
    private val _actionFrames = Array<ActionFrame>()
    private val _weightSlotPose = HashMap<String, FloatArray>()

    private val _weightBonePoses = HashMap<String, FloatArray>()
    private val _weightBoneIndices = HashMap<String, IntArray>()

    private val _cacheBones = HashMap<String, Array<BoneData>>()
    private val _meshs = HashMap<String, MeshDisplayData>()
    private val _slotChildActions = HashMap<String, Array<ActionData>>()

    // private readonly _intArray: Array<number> = [];
    // private readonly _floatArray: Array<number> = [];
    // private readonly _frameIntArray: Array<number> = [];
    // private readonly _frameFloatArray: Array<number> = [];
    // private readonly _frameArray: Array<number> = [];
    // private readonly _timelineArray: Array<number> = [];

    /**
     * @private
     */
    private fun _getCurvePoint(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
        x4: Float,
        y4: Float,
        t: Float,
        result: Point
    ) {
        val l_t = 1f - t
        val powA = l_t * l_t
        val powB = t * t
        val kA = l_t * powA
        val kB = (3.0 * t.toDouble() * powA.toDouble()).toFloat()
        val kC = (3.0 * l_t.toDouble() * powB.toDouble()).toFloat()
        val kD = t * powB

        result.x = kA * x1 + kB * x2 + kC * x3 + kD * x4
        result.y = kA * y1 + kB * y2 + kC * y3 + kD * y4
    }

    /**
     * @private
     */
    private fun _samplingEasingCurve(curve: FloatArray, samples: FloatArray) {
        val curveCount = curve.length()
        var stepIndex = -2
        var i = 0
        val l = samples.length()
        while (i < l) {
            val t = (i + 1) / (l + 1)
            while ((if (stepIndex + 6 < curveCount) curve[stepIndex + 6] else 1) < t) { // stepIndex + 3 * 2
                stepIndex += 6
            }

            val isInCurve = stepIndex >= 0 && stepIndex + 6 < curveCount
            val x1 = if (isInCurve) curve[stepIndex] else 0f
            val y1 = if (isInCurve) curve[stepIndex + 1] else 0f
            val x2 = curve[stepIndex + 2]
            val y2 = curve[stepIndex + 3]
            val x3 = curve[stepIndex + 4]
            val y3 = curve[stepIndex + 5]
            val x4 = if (isInCurve) curve[stepIndex + 6] else 1f
            val y4 = if (isInCurve) curve[stepIndex + 7] else 1f

            var lower = 0f
            var higher = 1f
            while (higher - lower > 0.0001) {
                val percentage = (higher + lower) * 0.5f
                this._getCurvePoint(x1, y1, x2, y2, x3, y3, x4, y4, percentage, this._helpPoint)
                if (t - this._helpPoint.x > 0f) {
                    lower = percentage
                } else {
                    higher = percentage
                }
            }

            samples[i] = this._helpPoint.y
            ++i
        }
    }

    private fun _sortActionFrame(a: ActionFrame, b: ActionFrame): Int {
        return if (a.frameStart > b.frameStart) 1 else -1
    }

    private fun _parseActionDataInFrame(rawData: Any, frameStart: Int, bone: BoneData?, slot: SlotData?) {
        if (`in`(rawData, ObjectDataParser.EVENT)) {
            this._mergeActionFrame(get(rawData, ObjectDataParser.EVENT), frameStart, ActionType.Frame, bone, slot)
        }

        if (`in`(rawData, ObjectDataParser.SOUND)) {
            this._mergeActionFrame(get(rawData, ObjectDataParser.SOUND), frameStart, ActionType.Sound, bone, slot)
        }

        if (`in`(rawData, ObjectDataParser.ACTION)) {
            this._mergeActionFrame(get(rawData, ObjectDataParser.ACTION), frameStart, ActionType.Play, bone, slot)
        }

        if (`in`(rawData, ObjectDataParser.EVENTS)) {
            this._mergeActionFrame(get(rawData, ObjectDataParser.EVENTS), frameStart, ActionType.Frame, bone, slot)
        }

        if (`in`(rawData, ObjectDataParser.ACTIONS)) {
            this._mergeActionFrame(get(rawData, ObjectDataParser.ACTIONS), frameStart, ActionType.Play, bone, slot)
        }
    }

    private fun _mergeActionFrame(rawData: Any?, frameStart: Int, type: ActionType, bone: BoneData?, slot: SlotData?) {
        val actionOffset = this._armature!!.actions.size()
        val actionCount = this._parseActionData(rawData, this._armature!!.actions, type, bone, slot)
        var frame: ActionFrame? = null

        if (this._actionFrames.size() == 0) { // First frame.
            frame = ActionFrame()
            frame.frameStart = 0
            this._actionFrames.add(frame)
            frame = null
        }

        for (eachFrame in this._actionFrames) { // Get same frame.
            if (eachFrame.frameStart == frameStart) {
                frame = eachFrame
                break
            }
        }

        if (frame == null) { // Create and cache frame.
            frame = ActionFrame()
            frame.frameStart = frameStart
            this._actionFrames.add(frame)
        }

        for (i in 0 until actionCount) { // Cache action offsets.
            frame.actions.push(actionOffset + i)
        }
    }

    private fun _parseCacheActionFrame(frame: ActionFrame): Int {
        val frameArray = this._data!!.frameArray
        val frameOffset = frameArray!!.length()
        val actionCount = frame.actions.size()
        frameArray.incrementLength(1 + 1 + actionCount)
        frameArray.set(frameOffset + BinaryOffset.FramePosition.v, frame.frameStart)
        frameArray.set(frameOffset + BinaryOffset.FramePosition.v + 1, actionCount) // Action count.

        for (i in 0 until actionCount) { // Action offsets.
            frameArray.set(frameOffset + BinaryOffset.FramePosition.v + 2 + i, frame.actions.get(i))
        }

        return frameOffset
    }

    /**
     * @private
     */
    protected fun _parseArmature(rawData: Any, scale: Float): ArmatureData {
        val armature = BaseObject.borrowObject(ArmatureData::class.java)
        armature.name = getString(rawData, ObjectDataParser.NAME, "")
        armature.frameRate = getFloat(rawData, ObjectDataParser.FRAME_RATE, this._data!!.frameRate)
        armature.scale = scale

        if (`in`(rawData, ObjectDataParser.TYPE) && get(rawData, ObjectDataParser.TYPE) is String) {
            armature.type = ObjectDataParser._getArmatureType(Objects.toString(get(rawData, ObjectDataParser.TYPE)))
        } else {
            armature.type = ArmatureType.values[getInt(rawData, ObjectDataParser.TYPE, ArmatureType.Armature.v)]
        }

        if (armature.frameRate == 0f) { // Data error.
            armature.frameRate = 24f
        }

        this._armature = armature

        if (`in`(rawData, ObjectDataParser.AABB)) {
            val rawAABB = get(rawData, ObjectDataParser.AABB)
            armature.aabb.x = getFloat(rawAABB, ObjectDataParser.X, 0f)
            armature.aabb.y = getFloat(rawAABB, ObjectDataParser.Y, 0f)
            armature.aabb.width = getFloat(rawAABB, ObjectDataParser.WIDTH, 0f)
            armature.aabb.height = getFloat(rawAABB, ObjectDataParser.HEIGHT, 0f)
        }

        if (`in`(rawData, ObjectDataParser.CANVAS)) {
            val rawCanvas = get(rawData, ObjectDataParser.CANVAS)
            val canvas = BaseObject.borrowObject(CanvasData::class.java)

            if (`in`(rawCanvas, ObjectDataParser.COLOR)) {
                getFloat(rawCanvas, ObjectDataParser.COLOR, 0f)
                canvas.hasBackground = true
            } else {
                canvas.hasBackground = false
            }

            canvas.color = getInt(rawCanvas, ObjectDataParser.COLOR, 0)
            canvas.x = getFloat(rawCanvas, ObjectDataParser.X, 0f)
            canvas.y = getFloat(rawCanvas, ObjectDataParser.Y, 0f)
            canvas.width = getFloat(rawCanvas, ObjectDataParser.WIDTH, 0f)
            canvas.height = getFloat(rawCanvas, ObjectDataParser.HEIGHT, 0f)

            armature.canvas = canvas
        }

        if (`in`(rawData, ObjectDataParser.BONE)) {
            val rawBones = getArray<Any>(rawData, ObjectDataParser.BONE)
            for (rawBone in rawBones!!) {
                val parentName = getString(rawBone, ObjectDataParser.PARENT, "")
                val bone = this._parseBone(rawBone)

                if (parentName!!.length > 0) { // Get bone parent.
                    val parent = armature.getBone(parentName)
                    if (parent != null) {
                        bone.parent = parent
                    } else { // Cache.
                        if (!this._cacheBones.containsKey(parentName)) {
                            this._cacheBones[parentName] = Array<BoneData>()
                        }
                        this._cacheBones[parentName].push(bone)
                    }
                }

                if (`in`(this._cacheBones, bone.name)) {
                    for (child in this._cacheBones[bone.name]) {
                        child.parent = bone
                    }

                    this._cacheBones.remove(bone.name)
                }

                armature.addBone(bone)

                this._rawBones.add(bone) // Raw bone sort.
            }
        }

        if (`in`(rawData, ObjectDataParser.IK)) {
            val rawIKS = getArray<Any>(rawData, ObjectDataParser.IK)
            for (rawIK in rawIKS!!) {
                this._parseIKConstraint(rawIK)
            }
        }

        armature.sortBones()

        if (`in`(rawData, ObjectDataParser.SLOT)) {
            val rawSlots = getArray<Any>(rawData, ObjectDataParser.SLOT)
            for (rawSlot in rawSlots!!) {
                armature.addSlot(this._parseSlot(rawSlot))
            }
        }

        if (`in`(rawData, ObjectDataParser.SKIN)) {
            val rawSkins = getArray<Any>(rawData, ObjectDataParser.SKIN)
            for (rawSkin in rawSkins!!) {
                armature.addSkin(this._parseSkin(rawSkin))
            }
        }

        if (`in`(rawData, ObjectDataParser.ANIMATION)) {
            val rawAnimations = getArray<Any>(rawData, ObjectDataParser.ANIMATION)
            for (rawAnimation in rawAnimations!!) {
                val animation = this._parseAnimation(rawAnimation)
                armature.addAnimation(animation)
            }
        }

        if (`in`(rawData, ObjectDataParser.DEFAULT_ACTIONS)) {
            this._parseActionData(
                get(rawData, ObjectDataParser.DEFAULT_ACTIONS),
                armature.defaultActions,
                ActionType.Play,
                null,
                null
            )
        }

        if (`in`(rawData, ObjectDataParser.ACTIONS)) {
            this._parseActionData(get(rawData, ObjectDataParser.ACTIONS), armature.actions, ActionType.Play, null, null)
        }

        for (i in 0 until armature.defaultActions.size()) {
            val action = armature.defaultActions.get(i)
            if (action.type == ActionType.Play) {
                val animation = armature.getAnimation(action.name)
                if (animation != null) {
                    armature.defaultAnimation = animation
                }
                break
            }
        }

        // Clear helper.
        this._rawBones.clear()
        this._armature = null
        this._meshs.clear()
        this._cacheBones.clear()
        this._slotChildActions.clear()
        this._weightSlotPose.clear()
        this._weightBonePoses.clear()
        this._weightBoneIndices.clear()
        return armature
    }

    /**
     * @private
     */
    protected fun _parseBone(rawData: Any): BoneData {
        val bone = BaseObject.borrowObject(BoneData::class.java)
        bone.inheritTranslation = getBool(rawData, ObjectDataParser.INHERIT_TRANSLATION, true)
        bone.inheritRotation = getBool(rawData, ObjectDataParser.INHERIT_ROTATION, true)
        bone.inheritScale = getBool(rawData, ObjectDataParser.INHERIT_SCALE, true)
        bone.inheritReflection = getBool(rawData, ObjectDataParser.INHERIT_REFLECTION, true)
        bone.length = getFloat(rawData, ObjectDataParser.LENGTH, 0f) * this._armature!!.scale
        bone.name = getString(rawData, ObjectDataParser.NAME, "")

        if (`in`(rawData, ObjectDataParser.TRANSFORM)) {
            this._parseTransform(get(rawData, ObjectDataParser.TRANSFORM), bone.transform, this._armature!!.scale)
        }

        return bone
    }

    /**
     * @private
     */
    protected fun _parseIKConstraint(rawData: Any) {
        val bone = this._armature!!.getBone(
            getString(
                rawData,
                if (`in`(rawData, ObjectDataParser.BONE)) ObjectDataParser.BONE else ObjectDataParser.NAME,
                ""
            )
        ) ?: return

        val target = this._armature!!.getBone(getString(rawData, ObjectDataParser.TARGET, "")) ?: return

        val constraint = BaseObject.borrowObject(IKConstraintData::class.java)
        constraint.bendPositive = getBool(rawData, ObjectDataParser.BEND_POSITIVE, true)
        constraint.scaleEnabled = getBool(rawData, ObjectDataParser.SCALE, false)
        constraint.weight = getFloat(rawData, ObjectDataParser.WEIGHT, 1f)
        constraint.bone = bone
        constraint.target = target

        val chain = getFloat(rawData, ObjectDataParser.CHAIN, 0f)
        if (chain > 0) {
            constraint.root = bone.parent
        }
        bone.constraints.add(constraint)
    }

    /**
     * @private
     */
    protected fun _parseSlot(rawData: Any): SlotData {
        val slot = BaseObject.borrowObject(SlotData::class.java)
        slot.displayIndex = getInt(rawData, ObjectDataParser.DISPLAY_INDEX, 0)
        slot.zOrder = this._armature!!.sortedSlots.size().toFloat()
        slot.name = getString(rawData, ObjectDataParser.NAME, "")
        slot.parent = this._armature!!.getBone(getString(rawData, ObjectDataParser.PARENT, "")) //

        if (`in`(rawData, ObjectDataParser.BLEND_MODE) && get(rawData, ObjectDataParser.BLEND_MODE) is String) {
            slot.blendMode = ObjectDataParser._getBlendMode(Objects.toString(get(rawData, ObjectDataParser.BLEND_MODE)))
        } else {
            slot.blendMode = BlendMode.values[getInt(rawData, ObjectDataParser.BLEND_MODE, BlendMode.Normal.v)]
        }

        if (`in`(rawData, ObjectDataParser.COLOR)) {
            // slot.color = SlotData.createColor();
            slot.color = SlotData.createColor()
            this._parseColorTransform(get(rawData, ObjectDataParser.COLOR), slot.color)
        } else {
            // slot.color = SlotData.DEFAULT_COLOR;
            slot.color = SlotData.DEFAULT_COLOR
        }

        if (`in`(rawData, ObjectDataParser.ACTIONS)) {
            val actions = Array<ActionData>()
            this._slotChildActions[slot.name] = actions
            this._parseActionData(get(rawData, ObjectDataParser.ACTIONS), actions, ActionType.Play, null, null)
        }

        return slot
    }

    /**
     * @private
     */
    protected fun _parseSkin(rawData: Any): SkinData {
        val skin = BaseObject.borrowObject(SkinData::class.java)
        skin.name = getString(rawData, ObjectDataParser.NAME, ObjectDataParser.DEFAULT_NAME)
        if (skin.name.length == 0) {
            skin.name = ObjectDataParser.DEFAULT_NAME
        }

        if (`in`(rawData, ObjectDataParser.SLOT)) {
            this._skin = skin

            val rawSlots = getArray<Any>(rawData, ObjectDataParser.SLOT)
            for (rawSlot in rawSlots!!) {
                val slotName = getString(rawSlot, ObjectDataParser.NAME, "")
                val slot = this._armature!!.getSlot(slotName)
                if (slot != null) {
                    this._slot = slot

                    if (`in`(rawSlot, ObjectDataParser.DISPLAY)) {
                        val rawDisplays = getArray<Any>(rawSlot, ObjectDataParser.DISPLAY)
                        for (rawDisplay in rawDisplays!!) {
                            skin.addDisplay(slotName, this._parseDisplay(rawDisplay))
                        }
                    }

                    this._slot = null //
                }
            }

            this._skin = null //
        }

        return skin
    }

    /**
     * @private
     */
    protected fun _parseDisplay(rawData: Any): DisplayData? {
        var display: DisplayData? = null
        val name = getString(rawData, ObjectDataParser.NAME, "")
        val path = getString(rawData, ObjectDataParser.PATH, "")
        var type = DisplayType.Image
        if (`in`(rawData, ObjectDataParser.TYPE) && get(rawData, ObjectDataParser.TYPE) is String) {
            type = ObjectDataParser._getDisplayType(getString(rawData, ObjectDataParser.TYPE, "")!!)
        } else {
            type = DisplayType.values[getInt(rawData, ObjectDataParser.TYPE, type.v)]
        }

        when (type) {
            DisplayType.Image -> {
                val imageDisplay = BaseObject.borrowObject(ImageDisplayData::class.java)
                display = imageDisplay
                imageDisplay.name = name
                imageDisplay.path = if (path!!.length > 0) path else name
                this._parsePivot(rawData, imageDisplay)
            }

            DisplayType.Armature -> {
                val armatureDisplay = BaseObject.borrowObject(ArmatureDisplayData::class.java)
                display = armatureDisplay
                armatureDisplay.name = name
                armatureDisplay.path = if (path!!.length > 0) path else name
                armatureDisplay.inheritAnimation = true

                if (`in`(rawData, ObjectDataParser.ACTIONS)) {
                    this._parseActionData(
                        get(rawData, ObjectDataParser.ACTIONS),
                        armatureDisplay.actions,
                        ActionType.Play,
                        null,
                        null
                    )
                } else if (`in`(this._slotChildActions, this._slot!!.name)) {
                    val displays = this._skin!!.getDisplays(this._slot!!.name)
                    if (if (displays == null) this._slot!!.displayIndex == 0 else this._slot!!.displayIndex == displays.length) {
                        for (action in this._slotChildActions[this._slot!!.name]) {
                            armatureDisplay.actions.push(action)
                        }

                        this._slotChildActions.remove(this._slot!!.name)
                    }
                }
            }

            DisplayType.Mesh -> {
                val meshDisplay = BaseObject.borrowObject(MeshDisplayData::class.java)
                display = meshDisplay
                meshDisplay.name = name
                meshDisplay.path = if (path!!.length > 0) path else name
                meshDisplay.inheritAnimation = getBool(rawData, ObjectDataParser.INHERIT_FFD, true)
                this._parsePivot(rawData, meshDisplay)

                val shareName = getString(rawData, ObjectDataParser.SHARE, "")
                if (shareName!!.length > 0) {
                    val shareMesh = this._meshs[shareName]
                    meshDisplay.offset = shareMesh.offset
                    meshDisplay.weight = shareMesh.weight
                } else {
                    this._parseMesh(rawData, meshDisplay)
                    this._meshs[meshDisplay.name] = meshDisplay
                }
            }

            DisplayType.BoundingBox -> {
                val boundingBox = this._parseBoundingBox(rawData)
                if (boundingBox != null) {
                    val boundingBoxDisplay = BaseObject.borrowObject(BoundingBoxDisplayData::class.java)
                    display = boundingBoxDisplay
                    boundingBoxDisplay.name = name
                    boundingBoxDisplay.path = if (path!!.length > 0) path else name
                    boundingBoxDisplay.boundingBox = boundingBox
                }
            }
        }

        if (display != null) {
            display.parent = this._armature
            if (`in`(rawData, ObjectDataParser.TRANSFORM)) {
                this._parseTransform(
                    get(rawData, ObjectDataParser.TRANSFORM),
                    display.transform,
                    this._armature!!.scale
                )
            }
        }

        return display
    }

    /**
     * @private
     */
    protected fun _parsePivot(rawData: Any, display: ImageDisplayData) {
        if (`in`(rawData, ObjectDataParser.PIVOT)) {
            val rawPivot = get(rawData, ObjectDataParser.PIVOT)
            display.pivot.x = getFloat(rawPivot, ObjectDataParser.X, 0f)
            display.pivot.y = getFloat(rawPivot, ObjectDataParser.Y, 0f)
        } else {
            display.pivot.x = 0.5f
            display.pivot.y = 0.5f
        }
    }

    /**
     * @private
     */
    protected open fun _parseMesh(rawData: Any, mesh: MeshDisplayData) {
        val rawVertices = getFloatArray(rawData, ObjectDataParser.VERTICES)
        val rawUVs = getFloatArray(rawData, ObjectDataParser.UVS)
        val rawTriangles = getIntArray(rawData, ObjectDataParser.TRIANGLES)
        val intArray = this._data!!.intArray
        val floatArray = this._data!!.floatArray
        val vertexCount = Math.floor((rawVertices!!.size() / 2).toDouble()).toInt() // uint
        val triangleCount = Math.floor((rawTriangles!!.size() / 3).toDouble()).toInt() // uint
        val vertexOffset = floatArray!!.length()
        val uvOffset = vertexOffset + vertexCount * 2

        mesh.offset = intArray!!.length()
        intArray.incrementLength(1 + 1 + 1 + 1 + triangleCount * 3)
        intArray.set(mesh.offset + BinaryOffset.MeshVertexCount.v, vertexCount)
        intArray.set(mesh.offset + BinaryOffset.MeshTriangleCount.v, triangleCount)
        intArray.set(mesh.offset + BinaryOffset.MeshFloatOffset.v, vertexOffset)
        run {
            var i = 0
            val l = triangleCount * 3
            while (i < l) {
                intArray.set(mesh.offset + BinaryOffset.MeshVertexIndices.v + i, rawTriangles.get(i))
                ++i
            }
        }

        floatArray.incrementLength(vertexCount * 2 + vertexCount * 2)
        run {
            var i = 0
            val l = vertexCount * 2
            while (i < l) {
                floatArray.set(vertexOffset + i, rawVertices.get(i))
                floatArray.set(uvOffset + i, rawUVs!!.get(i))
                ++i
            }
        }

        if (`in`(rawData, ObjectDataParser.WEIGHTS)) {

            val rawWeights = getIntArray(rawData, ObjectDataParser.WEIGHTS)
            val rawSlotPose = getFloatArray(rawData, ObjectDataParser.SLOT_POSE)
            // @TODO: Java: Used as int and as float?
            val rawBonePoses = getFloatArray(rawData, ObjectDataParser.BONE_POSE)
            val weightBoneIndices = IntArray()
            val weightBoneCount = Math.floor((rawBonePoses!!.size() / 7).toDouble()).toInt() // uint
            val floatOffset = floatArray.length()
            val weight = BaseObject.borrowObject(WeightData::class.java)

            weight.count = (rawWeights!!.size() - vertexCount) / 2
            weight.offset = intArray.length()
            weight.bones.length = weightBoneCount
            weightBoneIndices.length = weightBoneCount
            intArray.incrementLength(1 + 1 + weightBoneCount + vertexCount + weight.count)
            intArray.set(weight.offset + BinaryOffset.WeigthFloatOffset.v, floatOffset)

            for (i in 0 until weightBoneCount) {
                val rawBoneIndex = rawBonePoses.get(i * 7).toInt() // uint
                val bone = this._rawBones.get(rawBoneIndex)
                weight.bones.set(i, bone)
                weightBoneIndices.set(i, rawBoneIndex)

                intArray.set(
                    weight.offset + BinaryOffset.WeigthBoneIndices.v + i,
                    this._armature!!.sortedBones.indexOf(bone)
                )
            }

            floatArray.incrementLength(weight.count * 3)
            this._helpMatrixA.copyFromArray(rawSlotPose, 0)

            var i = 0
            var iW = 0
            var iB = weight.offset + BinaryOffset.WeigthBoneIndices.v + weightBoneCount
            var iV = floatOffset
            while (i < vertexCount) {
                val iD = i * 2
                val vertexBoneCount = rawWeights.get(iW++) // uint
                intArray.set(iB++, vertexBoneCount)

                var x = floatArray.get(vertexOffset + iD)
                var y = floatArray.get(vertexOffset + iD + 1)
                this._helpMatrixA.transformPoint(x, y, this._helpPoint)
                x = this._helpPoint.x
                y = this._helpPoint.y

                for (j in 0 until vertexBoneCount) {
                    val rawBoneIndex = rawWeights.get(iW++) // uint
                    val bone = this._rawBones.get(rawBoneIndex)
                    this._helpMatrixB.copyFromArray(rawBonePoses, weightBoneIndices.indexOf(rawBoneIndex) * 7 + 1)
                    this._helpMatrixB.invert()
                    this._helpMatrixB.transformPoint(x, y, this._helpPoint)
                    intArray.set(iB++, weight.bones.indexOf(bone))
                    floatArray.set(iV++, rawWeights.get(iW++).toFloat())
                    floatArray.set(iV++, this._helpPoint.x)
                    floatArray.set(iV++, this._helpPoint.y)
                }
                ++i
            }

            mesh.weight = weight

            //
            this._weightSlotPose[mesh.name] = rawSlotPose
            this._weightBonePoses[mesh.name] = rawBonePoses
            this._weightBoneIndices[mesh.name] = weightBoneIndices
        }
    }

    /**
     * @private
     */
    protected fun _parseBoundingBox(rawData: Any): BoundingBoxData? {
        var boundingBox: BoundingBoxData? = null
        var type = BoundingBoxType.Rectangle
        if (`in`(rawData, ObjectDataParser.SUB_TYPE) && get(rawData, ObjectDataParser.SUB_TYPE) is String) {
            type = ObjectDataParser._getBoundingBoxType(getString(rawData, ObjectDataParser.SUB_TYPE)!!)
        } else {
            type = BoundingBoxType.values[getInt(rawData, ObjectDataParser.SUB_TYPE, type.v)]
        }

        when (type) {
            BoundingBoxType.Rectangle ->
                // boundingBox = BaseObject.borrowObject(RectangleBoundingBoxData);
                boundingBox = BaseObject.borrowObject(RectangleBoundingBoxData::class.java)

            BoundingBoxType.Ellipse ->
                // boundingBox = BaseObject.borrowObject(EllipseBoundingBoxData);
                boundingBox = BaseObject.borrowObject(EllipseBoundingBoxData::class.java)

            BoundingBoxType.Polygon -> boundingBox = this._parsePolygonBoundingBox(rawData)
        }

        if (boundingBox != null) {
            boundingBox.color = getInt(rawData, ObjectDataParser.COLOR, 0x000000)
            if (boundingBox.type == BoundingBoxType.Rectangle || boundingBox.type == BoundingBoxType.Ellipse) {
                boundingBox.width = getFloat(rawData, ObjectDataParser.WIDTH, 0f)
                boundingBox.height = getFloat(rawData, ObjectDataParser.HEIGHT, 0f)
            }
        }

        return boundingBox
    }

    /**
     * @private
     */
    protected open fun _parsePolygonBoundingBox(rawData: Any): PolygonBoundingBoxData {
        val rawVertices = getFloatArray(rawData, ObjectDataParser.VERTICES)
        val floatArray = this._data!!.floatArray
        val polygonBoundingBox = BaseObject.borrowObject(PolygonBoundingBoxData::class.java)

        polygonBoundingBox.offset = floatArray!!.length()
        polygonBoundingBox.count = rawVertices!!.size()
        polygonBoundingBox.vertices = floatArray
        floatArray.incrementLength(polygonBoundingBox.count)

        var i = 0
        val l = polygonBoundingBox.count
        while (i < l) {
            val iN = i + 1
            val x = rawVertices.get(i)
            val y = rawVertices.get(iN)
            floatArray.set(polygonBoundingBox.offset + i, x)
            floatArray.set(polygonBoundingBox.offset + iN, y)

            // AABB.
            if (i == 0) {
                polygonBoundingBox.x = x
                polygonBoundingBox.y = y
                polygonBoundingBox.width = x
                polygonBoundingBox.height = y
            } else {
                if (x < polygonBoundingBox.x) {
                    polygonBoundingBox.x = x
                } else if (x > polygonBoundingBox.width) {
                    polygonBoundingBox.width = x
                }

                if (y < polygonBoundingBox.y) {
                    polygonBoundingBox.y = y
                } else if (y > polygonBoundingBox.height) {
                    polygonBoundingBox.height = y
                }
            }
            i += 2
        }

        return polygonBoundingBox
    }

    /**
     * @private
     */
    protected open fun _parseAnimation(rawData: Any): AnimationData {
        // const animation = BaseObject.borrowObject(AnimationData);
        val animation = BaseObject.borrowObject(AnimationData::class.java)
        animation.frameCount = Math.max(getInt(rawData, ObjectDataParser.DURATION, 1), 1)
        animation.playTimes = getInt(rawData, ObjectDataParser.PLAY_TIMES, 1)
        animation.duration = animation.frameCount / this._armature!!.frameRate
        animation.fadeInTime = getFloat(rawData, ObjectDataParser.FADE_IN_TIME, 0f)
        animation.scale = getFloat(rawData, ObjectDataParser.SCALE, 1f)
        animation.name = getString(rawData, ObjectDataParser.NAME, ObjectDataParser.DEFAULT_NAME)
        // TDOO Check std::string length
        if (animation.name.length < 1) {
            animation.name = ObjectDataParser.DEFAULT_NAME
        }
        animation.frameIntOffset = this._data!!.frameIntArray!!.length()
        animation.frameFloatOffset = this._data!!.frameFloatArray!!.length()
        animation.frameOffset = this._data!!.frameArray!!.length()

        this._animation = animation

        if (`in`(rawData, ObjectDataParser.FRAME)) {
            val rawFrames = getArray<Any>(rawData, ObjectDataParser.FRAME)
            val keyFrameCount = rawFrames!!.size()
            if (keyFrameCount > 0) {
                var i = 0
                var frameStart = 0
                while (i < keyFrameCount) {
                    val rawFrame = rawFrames.get(i)
                    this._parseActionDataInFrame(rawFrame, frameStart, null, null)
                    frameStart += getFloat(rawFrame, ObjectDataParser.DURATION, 1f).toInt()
                    ++i
                }
            }
        }

        if (`in`(rawData, ObjectDataParser.Z_ORDER)) {
            this._animation!!.zOrderTimeline = this._parseTimeline(
                get(rawData, ObjectDataParser.Z_ORDER), TimelineType.ZOrder,
                false, false, 0,
                FrameParser { rawData, frameStart, frameCount ->
                    this._parseZOrderFrame(
                        rawData,
                        frameStart,
                        frameCount
                    )
                }
            )
        }

        if (`in`(rawData, ObjectDataParser.BONE)) {
            val rawTimelines = getArray<Any>(rawData, ObjectDataParser.BONE)
            for (rawTimeline in rawTimelines!!) {
                this._parseBoneTimeline(rawTimeline)
            }
        }

        if (`in`(rawData, ObjectDataParser.SLOT)) {
            val rawTimelines = getArray<Any>(rawData, ObjectDataParser.SLOT)
            for (rawTimeline in rawTimelines!!) {
                this._parseSlotTimeline(rawTimeline)
            }
        }

        if (`in`(rawData, ObjectDataParser.FFD)) {
            val rawTimelines = getArray<Any>(rawData, ObjectDataParser.FFD)
            for (rawTimeline in rawTimelines!!) {
                val slotName = getString(rawTimeline, ObjectDataParser.SLOT, "")
                val displayName = getString(rawTimeline, ObjectDataParser.NAME, "")
                val slot = this._armature!!.getSlot(slotName) ?: continue

                this._slot = slot
                this._mesh = this._meshs[displayName]

                val timelineFFD = this._parseTimeline(
                    rawTimeline,
                    TimelineType.SlotFFD,
                    false,
                    true,
                    0,
                    FrameParser { rawData, frameStart, frameCount ->
                        this._parseSlotFFDFrame(
                            rawData,
                            frameStart,
                            frameCount
                        )
                    })
                if (timelineFFD != null) {
                    this._animation!!.addSlotTimeline(slot, timelineFFD)
                }

                this._slot = null //
                this._mesh = null //
            }
        }

        if (this._actionFrames.size() > 0) {
            this._actionFrames.sort { a, b -> this._sortActionFrame(a, b) }

            this._animation!!.actionTimeline = BaseObject.borrowObject(TimelineData::class.java)
            val timeline = this._animation!!.actionTimeline
            val timelineArray = this._data!!.timelineArray
            val keyFrameCount = this._actionFrames.size()
            timeline.type = TimelineType.Action
            timeline.offset = timelineArray!!.length()
            timelineArray.incrementLength(1 + 1 + 1 + 1 + 1 + keyFrameCount)
            timelineArray.set(timeline.offset + BinaryOffset.TimelineScale.v, 100)
            timelineArray.set(timeline.offset + BinaryOffset.TimelineOffset.v, 0)
            timelineArray.set(timeline.offset + BinaryOffset.TimelineKeyFrameCount.v, keyFrameCount)
            timelineArray.set(timeline.offset + BinaryOffset.TimelineFrameValueCount.v, 0)
            timelineArray.set(timeline.offset + BinaryOffset.TimelineFrameValueOffset.v, 0)

            this._timeline = timeline

            if (keyFrameCount == 1) {
                timeline.frameIndicesOffset = -1
                timelineArray.set(
                    timeline.offset + BinaryOffset.TimelineFrameOffset.v + 0,
                    this._parseCacheActionFrame(this._actionFrames.get(0)) - this._animation!!.frameOffset
                )
            } else {
                val totalFrameCount = this._animation!!.frameCount + 1 // One more frame than animation.
                val frameIndices = this._data!!.frameIndices
                timeline.frameIndicesOffset = frameIndices.length()
                frameIndices.incrementLength(totalFrameCount)

                var i = 0
                var iK = 0
                var frameStart = 0
                var frameCount = 0
                while (i < totalFrameCount) {
                    if (frameStart + frameCount <= i && iK < keyFrameCount) {
                        val frame = this._actionFrames.get(iK)
                        frameStart = frame.frameStart
                        if (iK == keyFrameCount - 1) {
                            frameCount = this._animation!!.frameCount - frameStart
                        } else {
                            frameCount = this._actionFrames.get(iK + 1).frameStart - frameStart
                        }

                        timelineArray.set(
                            timeline.offset + BinaryOffset.TimelineFrameOffset.v + iK,
                            this._parseCacheActionFrame(frame) - this._animation!!.frameOffset
                        )
                        iK++
                    }

                    frameIndices.set(timeline.frameIndicesOffset + i, iK - 1)
                    ++i
                }
            }

            this._timeline = null //
            this._actionFrames.clear()
        }

        this._animation = null //

        return animation
    }

    internal interface FrameParser {
        fun parse(rawData: Any, frameStart: Int, frameCount: Int): Int
    }

    /**
     * @private
     */
    protected fun _parseTimeline(
        rawData: Any?, type: TimelineType,
        addIntOffset: Boolean, addFloatOffset: Boolean, frameValueCount: Int,
        frameParser: FrameParser
    ): TimelineData? {
        if (!`in`(rawData, ObjectDataParser.FRAME)) {
            return null
        }

        val rawFrames = getArray<Any>(rawData, ObjectDataParser.FRAME)
        val keyFrameCount = rawFrames!!.size()
        if (keyFrameCount == 0) {
            return null
        }

        val timelineArray = this._data!!.timelineArray
        val frameIntArrayLength = this._data!!.frameIntArray!!.length()
        val frameFloatArrayLength = this._data!!.frameFloatArray!!.length()
        val timeline = BaseObject.borrowObject(TimelineData::class.java)
        timeline.type = type
        timeline.offset = timelineArray!!.length()
        timelineArray.incrementLength(1 + 1 + 1 + 1 + 1 + keyFrameCount)
        timelineArray.set(
            timeline.offset + BinaryOffset.TimelineScale.v,
            Math.round(getFloat(rawData, ObjectDataParser.SCALE, 1f) * 100)
        )
        timelineArray.set(
            timeline.offset + BinaryOffset.TimelineOffset.v,
            Math.round(getFloat(rawData, ObjectDataParser.OFFSET, 0f) * 100)
        )
        timelineArray.set(timeline.offset + BinaryOffset.TimelineKeyFrameCount.v, keyFrameCount)
        timelineArray.set(timeline.offset + BinaryOffset.TimelineFrameValueCount.v, frameValueCount)
        if (addIntOffset) {
            timelineArray.set(
                timeline.offset + BinaryOffset.TimelineFrameValueOffset.v,
                frameIntArrayLength - this._animation!!.frameIntOffset
            )
        } else if (addFloatOffset) {
            timelineArray.set(
                timeline.offset + BinaryOffset.TimelineFrameValueOffset.v,
                frameFloatArrayLength - this._animation!!.frameFloatOffset
            )
        } else {
            timelineArray.set(timeline.offset + BinaryOffset.TimelineFrameValueOffset.v, 0)
        }

        this._timeline = timeline

        if (keyFrameCount == 1) { // Only one frame.
            timeline.frameIndicesOffset = -1
            timelineArray.set(
                timeline.offset + BinaryOffset.TimelineFrameOffset.v + 0,
                frameParser.parse(rawFrames.get(0), 0, 0) - this._animation!!.frameOffset
            )
        } else {
            val frameIndices = this._data!!.frameIndices
            val totalFrameCount = this._animation!!.frameCount + 1 // One more frame than animation.
            timeline.frameIndicesOffset = frameIndices.size()
            frameIndices.incrementLength(totalFrameCount)

            var i = 0
            var iK = 0
            var frameStart = 0
            var frameCount = 0
            while (i < totalFrameCount) {
                if (frameStart + frameCount <= i && iK < keyFrameCount) {
                    val rawFrame = rawFrames.get(iK)
                    frameStart = i
                    frameCount = getInt(rawFrame, ObjectDataParser.DURATION, 1)
                    if (iK == keyFrameCount - 1) {
                        frameCount = this._animation!!.frameCount - frameStart
                    }

                    timelineArray.set(
                        timeline.offset + BinaryOffset.TimelineFrameOffset.v + iK,
                        frameParser.parse(rawFrame, frameStart, frameCount) - this._animation!!.frameOffset
                    )
                    iK++
                }

                frameIndices.set(timeline.frameIndicesOffset + i, iK - 1)
                ++i
            }
        }

        this._timeline = null //

        return timeline
    }

    /**
     * @private
     */
    protected fun _parseBoneTimeline(rawData: Any) {
        val bone = this._armature!!.getBone(getString(rawData, ObjectDataParser.NAME, "")) ?: return

        this._bone = bone
        this._slot = this._armature!!.getSlot(this._bone!!.name)

        val timeline = this._parseTimeline(
            rawData, TimelineType.BoneAll,
            false, true, 6,
            FrameParser { rawData, frameStart, frameCount -> this._parseBoneFrame(rawData, frameStart, frameCount) }
        )
        if (timeline != null) {
            this._animation!!.addBoneTimeline(bone, timeline)
        }

        this._bone = null //
        this._slot = null //
    }

    /**
     * @private
     */
    protected fun _parseSlotTimeline(rawData: Any) {
        val slot = this._armature!!.getSlot(getString(rawData, ObjectDataParser.NAME, "")) ?: return

        this._slot = slot

        val displayIndexTimeline = this._parseTimeline(
            rawData,
            TimelineType.SlotDisplay,
            false,
            false,
            0,
            FrameParser { rawData, frameStart, frameCount ->
                this._parseSlotDisplayIndexFrame(
                    rawData,
                    frameStart,
                    frameCount
                )
            })
        if (displayIndexTimeline != null) {
            this._animation!!.addSlotTimeline(slot, displayIndexTimeline)
        }

        val colorTimeline = this._parseTimeline(
            rawData,
            TimelineType.SlotColor,
            true,
            false,
            1,
            FrameParser { rawData, frameStart, frameCount ->
                this._parseSlotColorFrame(
                    rawData,
                    frameStart,
                    frameCount
                )
            })
        if (colorTimeline != null) {
            this._animation!!.addSlotTimeline(slot, colorTimeline)
        }

        this._slot = null //
    }

    /**
     * @private
     */
    protected fun _parseFrame(rawData: Any, frameStart: Int, frameCount: Int, frameArray: IntArray?): Int {
        val frameOffset = frameArray!!.size()
        frameArray.incrementLength(1)
        frameArray[frameOffset + BinaryOffset.FramePosition.v] = frameStart

        return frameOffset
    }

    /**
     * @private
     */
    protected fun _parseTweenFrame(rawData: Any, frameStart: Int, frameCount: Int, frameArray: IntArray?): Int {
        val frameOffset = this._parseFrame(rawData, frameStart, frameCount, frameArray)

        if (frameCount > 0) {
            if (`in`(rawData, ObjectDataParser.CURVE)) {
                val sampleCount = frameCount + 1
                this._helpArray.length = sampleCount
                this._samplingEasingCurve(getFloatArray(rawData, ObjectDataParser.CURVE)!!, this._helpArray)

                frameArray!!.incrementLength(1 + 1 + this._helpArray.length)
                frameArray[frameOffset + BinaryOffset.FrameTweenType.v] = TweenType.Curve.v
                frameArray[frameOffset + BinaryOffset.FrameTweenEasingOrCurveSampleCount.v] = sampleCount
                for (i in 0 until sampleCount) {
                    frameArray[frameOffset + BinaryOffset.FrameCurveSamples.v + i] =
                            Math.round(this._helpArray.get(i) * 10000.0).toInt()
                }
            } else {
                val noTween = -2.0f
                var tweenEasing = noTween
                if (`in`(rawData, ObjectDataParser.TWEEN_EASING)) {
                    tweenEasing = getFloat(rawData, ObjectDataParser.TWEEN_EASING, noTween)
                }

                if (tweenEasing == noTween) {
                    frameArray!!.incrementLength(1)
                    frameArray[frameOffset + BinaryOffset.FrameTweenType.v] = TweenType.None.v
                } else if (tweenEasing == 0f) {
                    frameArray!!.incrementLength(1)
                    frameArray[frameOffset + BinaryOffset.FrameTweenType.v] = TweenType.Line.v
                } else if (tweenEasing < 0f) {
                    frameArray!!.incrementLength(1 + 1)
                    frameArray[frameOffset + BinaryOffset.FrameTweenType.v] = TweenType.QuadIn.v
                    frameArray[frameOffset + BinaryOffset.FrameTweenEasingOrCurveSampleCount.v] =
                            Math.round(-tweenEasing * 100.0).toInt()
                } else if (tweenEasing <= 1f) {
                    frameArray!!.incrementLength(1 + 1)
                    frameArray[frameOffset + BinaryOffset.FrameTweenType.v] = TweenType.QuadOut.v
                    frameArray[frameOffset + BinaryOffset.FrameTweenEasingOrCurveSampleCount.v] =
                            Math.round(tweenEasing * 100.0).toInt()
                } else {
                    frameArray!!.incrementLength(1 + 1)
                    frameArray[frameOffset + BinaryOffset.FrameTweenType.v] = TweenType.QuadInOut.v
                    frameArray[frameOffset + BinaryOffset.FrameTweenEasingOrCurveSampleCount.v] =
                            Math.round(tweenEasing * 100.0 - 100.0).toInt()
                }
            }
        } else {
            frameArray!!.incrementLength(1)
            frameArray[frameOffset + BinaryOffset.FrameTweenType.v] = TweenType.None.v
        }

        return frameOffset
    }

    /**
     * @private
     */
    protected fun _parseZOrderFrame(rawData: Any, frameStart: Int, frameCount: Int): Int {
        val frameArray = this._data!!.frameArray
        val frameOffset = this._parseFrame(rawData, frameStart, frameCount, frameArray)

        if (`in`(rawData, ObjectDataParser.Z_ORDER)) {
            val rawZOrder = getIntArray(rawData, ObjectDataParser.Z_ORDER)
            if (rawZOrder!!.length() > 0) {
                val slotCount = this._armature!!.sortedSlots.length()
                val unchanged = IntArray(slotCount - rawZOrder.length() / 2)
                val zOrders = IntArray(slotCount)

                for (i in 0 until slotCount) {
                    zOrders[i] = -1
                }

                var originalIndex = 0
                var unchangedIndex = 0
                run {
                    var i = 0
                    val l = rawZOrder.length()
                    while (i < l) {
                        val slotIndex = rawZOrder.get(i)
                        val zOrderOffset = rawZOrder.get(i + 1)

                        while (originalIndex != slotIndex) {
                            unchanged[unchangedIndex++] = originalIndex++
                        }

                        zOrders[originalIndex + zOrderOffset] = originalIndex++
                        i += 2
                    }
                }

                while (originalIndex < slotCount) {
                    unchanged[unchangedIndex++] = originalIndex++
                }

                frameArray!!.incrementLength(1 + slotCount)
                frameArray.set(frameOffset + 1, slotCount)

                var i = slotCount
                while (i-- > 0) {
                    if (zOrders[i] == -1) {
                        frameArray.set(frameOffset + 2 + i, unchanged[--unchangedIndex])
                    } else {
                        frameArray.set(frameOffset + 2 + i, zOrders[i])
                    }
                }

                return frameOffset
            }
        }

        frameArray!!.incrementLength(1)
        frameArray.set(frameOffset + 1, 0)

        return frameOffset
    }

    /**
     * @private
     */
    protected fun _parseBoneFrame(rawData: Any, frameStart: Int, frameCount: Int): Int {
        val frameFloatArray = this._data!!.frameFloatArray
        val frameArray = this._data!!.frameArray
        val frameOffset = this._parseTweenFrame(rawData, frameStart, frameCount, frameArray)

        this._helpTransform.identity()
        if (`in`(rawData, ObjectDataParser.TRANSFORM)) {
            this._parseTransform(get(rawData, ObjectDataParser.TRANSFORM), this._helpTransform, 1f)
        }

        // Modify rotation.
        var rotation = this._helpTransform.rotation
        if (frameStart != 0) {
            if (this._prevTweenRotate == 0f) {
                rotation = this._prevRotation + Transform.normalizeRadian(rotation - this._prevRotation)
            } else {
                if (if (this._prevTweenRotate > 0) rotation >= this._prevRotation else rotation <= this._prevRotation) {
                    this._prevTweenRotate =
                            if (this._prevTweenRotate > 0) this._prevTweenRotate - 1 else this._prevTweenRotate + 1
                }

                rotation = this._prevRotation + rotation - this._prevRotation + Transform.PI_D * this._prevTweenRotate
            }
        }

        this._prevTweenRotate = getFloat(rawData, ObjectDataParser.TWEEN_ROTATE, 0f)
        this._prevRotation = rotation

        var frameFloatOffset = frameFloatArray!!.length()
        frameFloatArray.incrementLength(6)
        frameFloatArray.set(frameFloatOffset++, this._helpTransform.x)
        frameFloatArray.set(frameFloatOffset++, this._helpTransform.y)
        frameFloatArray.set(frameFloatOffset++, rotation)
        frameFloatArray.set(frameFloatOffset++, this._helpTransform.skew)
        frameFloatArray.set(frameFloatOffset++, this._helpTransform.scaleX)
        frameFloatArray.set(frameFloatOffset++, this._helpTransform.scaleY)

        this._parseActionDataInFrame(rawData, frameStart, this._bone, this._slot)

        return frameOffset
    }

    /**
     * @private
     */
    protected fun _parseSlotDisplayIndexFrame(rawData: Any, frameStart: Int, frameCount: Int): Int {
        val frameArray = this._data!!.frameArray
        val frameOffset = this._parseFrame(rawData, frameStart, frameCount, frameArray)

        frameArray!!.incrementLength(1)
        frameArray.set(frameOffset + 1, getInt(rawData, ObjectDataParser.DISPLAY_INDEX, 0))

        this._parseActionDataInFrame(rawData, frameStart, this._slot!!.parent, this._slot)

        return frameOffset
    }

    /**
     * @private
     */
    protected fun _parseSlotColorFrame(rawData: Any, frameStart: Int, frameCount: Int): Int {
        val intArray = this._data!!.intArray
        val frameIntArray = this._data!!.frameIntArray
        val frameArray = this._data!!.frameArray
        val frameOffset = this._parseTweenFrame(rawData, frameStart, frameCount, frameArray)

        var colorOffset = -1
        if (`in`(rawData, ObjectDataParser.COLOR)) {
            val rawColor = getArray<Any>(rawData, ObjectDataParser.COLOR)
            for (k in 0 until rawColor!!.length()) {
                this._parseColorTransform(rawColor, this._helpColorTransform)
                colorOffset = intArray!!.length()
                intArray.incrementLength(8)
                intArray.set(colorOffset++, Math.round(this._helpColorTransform.alphaMultiplier * 100))
                intArray.set(colorOffset++, Math.round(this._helpColorTransform.redMultiplier * 100))
                intArray.set(colorOffset++, Math.round(this._helpColorTransform.greenMultiplier * 100))
                intArray.set(colorOffset++, Math.round(this._helpColorTransform.blueMultiplier * 100))
                intArray.set(colorOffset++, Math.round(this._helpColorTransform.alphaOffset.toFloat()))
                intArray.set(colorOffset++, Math.round(this._helpColorTransform.redOffset.toFloat()))
                intArray.set(colorOffset++, Math.round(this._helpColorTransform.greenOffset.toFloat()))
                intArray.set(colorOffset++, Math.round(this._helpColorTransform.blueOffset.toFloat()))
                colorOffset -= 8
                break
            }
        }

        if (colorOffset < 0) {
            if (this._defalultColorOffset < 0) {
                colorOffset = intArray!!.length()
                this._defalultColorOffset = colorOffset
                intArray.incrementLength(8)
                intArray.set(colorOffset++, 100)
                intArray.set(colorOffset++, 100)
                intArray.set(colorOffset++, 100)
                intArray.set(colorOffset++, 100)
                intArray.set(colorOffset++, 0)
                intArray.set(colorOffset++, 0)
                intArray.set(colorOffset++, 0)
                intArray.set(colorOffset++, 0)
            }

            colorOffset = this._defalultColorOffset
        }

        val frameIntOffset = frameIntArray!!.length()
        frameIntArray.incrementLength(1)
        frameIntArray.set(frameIntOffset, colorOffset)

        return frameOffset
    }

    /**
     * @private
     */
    protected fun _parseSlotFFDFrame(rawData: Any, frameStart: Int, frameCount: Int): Int {
        val intArray = this._data!!.intArray
        val frameFloatArray = this._data!!.frameFloatArray
        val frameArray = this._data!!.frameArray
        val frameFloatOffset = frameFloatArray!!.length()
        val frameOffset = this._parseTweenFrame(rawData, frameStart, frameCount, frameArray)
        val rawVertices =
            if (`in`(rawData, ObjectDataParser.VERTICES)) getFloatArray(rawData, ObjectDataParser.VERTICES) else null
        val offset = getInt(rawData, ObjectDataParser.OFFSET, 0) // uint
        val vertexCount = intArray!!.get(this._mesh!!.offset + BinaryOffset.MeshVertexCount.v)

        var x = 0f
        var y = 0f
        var iB = 0
        var iV = 0
        if (this._mesh!!.weight != null) {
            val rawSlotPose = this._weightSlotPose[this._mesh!!.name]
            this._helpMatrixA.copyFromArray(rawSlotPose, 0)
            frameFloatArray.incrementLength(this._mesh!!.weight!!.count * 2)
            iB = this._mesh!!.weight!!.offset + BinaryOffset.WeigthBoneIndices.v + this._mesh!!.weight!!.bones.length()
        } else {
            frameFloatArray.incrementLength(vertexCount * 2)
        }

        var i = 0
        while (i < vertexCount * 2) {
            if (rawVertices == null) { // Fill 0.
                x = 0f
                y = 0f
            } else {
                if (i < offset || i - offset >= rawVertices.length()) {
                    x = 0f
                } else {
                    x = rawVertices.get(i - offset)
                }

                if (i + 1 < offset || i + 1 - offset >= rawVertices.length()) {
                    y = 0f
                } else {
                    y = rawVertices.get(i + 1 - offset)
                }
            }

            if (this._mesh!!.weight != null) { // If mesh is skinned, transform point by bone bind pose.
                val rawBonePoses = this._weightBonePoses[this._mesh!!.name]
                val weightBoneIndices = this._weightBoneIndices[this._mesh!!.name]
                val vertexBoneCount = intArray.get(iB++)

                this._helpMatrixA.transformPoint(x, y, this._helpPoint, true)
                x = this._helpPoint.x
                y = this._helpPoint.y

                for (j in 0 until vertexBoneCount) {
                    val boneIndex = intArray.get(iB++)
                    val bone = this._mesh!!.weight!!.bones.get(boneIndex)
                    val rawBoneIndex = this._rawBones.indexOf(bone)

                    this._helpMatrixB.copyFromArray(rawBonePoses, weightBoneIndices.indexOf(rawBoneIndex) * 7 + 1)
                    this._helpMatrixB.invert()
                    this._helpMatrixB.transformPoint(x, y, this._helpPoint, true)

                    frameFloatArray.set(frameFloatOffset + iV++, this._helpPoint.x)
                    frameFloatArray.set(frameFloatOffset + iV++, this._helpPoint.y)
                }
            } else {
                frameFloatArray.set(frameFloatOffset + i, x)
                frameFloatArray.set(frameFloatOffset + i + 1, y)
            }
            i += 2
        }

        if (frameStart == 0) {
            val frameIntArray = this._data!!.frameIntArray
            val timelineArray = this._data!!.timelineArray
            val frameIntOffset = frameIntArray!!.length()
            frameIntArray.incrementLength(1 + 1 + 1 + 1 + 1)
            frameIntArray.set(frameIntOffset + BinaryOffset.FFDTimelineMeshOffset.v, this._mesh!!.offset)
            frameIntArray.set(
                frameIntOffset + BinaryOffset.FFDTimelineFFDCount.v,
                frameFloatArray.length() - frameFloatOffset
            )
            frameIntArray.set(
                frameIntOffset + BinaryOffset.FFDTimelineValueCount.v,
                frameFloatArray.length() - frameFloatOffset
            )
            frameIntArray.set(frameIntOffset + BinaryOffset.FFDTimelineValueOffset.v, 0)
            frameIntArray.set(frameIntOffset + BinaryOffset.FFDTimelineFloatOffset.v, frameFloatOffset)
            timelineArray!!.set(
                this._timeline!!.offset + BinaryOffset.TimelineFrameValueCount.v,
                frameIntOffset - this._animation!!.frameIntOffset
            )
        }

        return frameOffset
    }

    /**
     * @private
     */
    protected fun _parseActionData(
        rawData: Any?,
        actions: Array<ActionData>,
        type: ActionType,
        bone: BoneData?,
        slot: SlotData?
    ): Int {
        var actionCount = 0

        if (rawData is String) {
            val action = BaseObject.borrowObject(ActionData::class.java)
            action.type = type
            action.name = rawData
            action.bone = bone
            action.slot = slot
            actions.push(action)
            actionCount++
        } else if (rawData is ArrayBase<*>) {
            for (rawAction in (rawData as ArrayBase<*>?)!!) {
                val action = BaseObject.borrowObject(ActionData::class.java)
                if (`in`(rawAction, ObjectDataParser.GOTO_AND_PLAY)) {
                    action.type = ActionType.Play
                    action.name = getString(rawAction, ObjectDataParser.GOTO_AND_PLAY, "")
                } else {
                    if (`in`(rawAction, ObjectDataParser.TYPE) && get(rawAction, ObjectDataParser.TYPE) is String) {
                        action.type = ObjectDataParser._getActionType(getString(rawAction, ObjectDataParser.TYPE)!!)
                    } else {
                        action.type = ActionType.values[getInt(rawAction, ObjectDataParser.TYPE, type.v)]
                    }

                    action.name = getString(rawAction, ObjectDataParser.NAME, "")
                }

                if (`in`(rawAction, ObjectDataParser.BONE)) {
                    val boneName = getString(rawAction, ObjectDataParser.BONE, "")
                    action.bone = this._armature!!.getBone(boneName)
                } else {
                    action.bone = bone
                }

                if (`in`(rawAction, ObjectDataParser.SLOT)) {
                    val slotName = getString(rawAction, ObjectDataParser.SLOT, "")
                    action.slot = this._armature!!.getSlot(slotName)
                } else {
                    action.slot = slot
                }

                if (`in`(rawAction, ObjectDataParser.INTS)) {
                    if (action.data == null) {
                        action.data = BaseObject.borrowObject(UserData::class.java)
                    }

                    val rawInts = getIntArray(rawAction, ObjectDataParser.INTS)
                    for (rawValue in rawInts!!) {
                        action.data!!.ints.push(rawValue)
                    }
                }

                if (`in`(rawAction, ObjectDataParser.FLOATS)) {
                    if (action.data == null) {
                        action.data = BaseObject.borrowObject(UserData::class.java)
                    }

                    val rawFloats = getFloatArray(rawAction, ObjectDataParser.FLOATS)
                    for (rawValue in rawFloats!!) {
                        action.data!!.floats.push(rawValue)
                    }
                }

                if (`in`(rawAction, ObjectDataParser.STRINGS)) {
                    if (action.data == null) {
                        action.data = BaseObject.borrowObject(UserData::class.java)
                    }

                    val rawStrings = getArray<String>(rawAction, ObjectDataParser.STRINGS)
                    for (rawValue in rawStrings!!) {
                        action.data!!.strings.push(rawValue)
                    }
                }

                actions.push(action)
                actionCount++
            }
        }

        return actionCount
    }

    /**
     * @private
     */
    protected fun _parseTransform(rawData: Any?, transform: Transform, scale: Float) {
        transform.x = getFloat(rawData, ObjectDataParser.X, 0f) * scale
        transform.y = getFloat(rawData, ObjectDataParser.Y, 0f) * scale

        if (`in`(rawData, ObjectDataParser.ROTATE) || `in`(rawData, ObjectDataParser.SKEW)) {
            transform.rotation =
                    Transform.normalizeRadian(getFloat(rawData, ObjectDataParser.ROTATE, 0f) * Transform.DEG_RAD)
            transform.skew = Transform.normalizeRadian(getFloat(rawData, ObjectDataParser.SKEW, 0f) * Transform.DEG_RAD)
        } else if (`in`(rawData, ObjectDataParser.SKEW_X) || `in`(rawData, ObjectDataParser.SKEW_Y)) {
            transform.rotation =
                    Transform.normalizeRadian(getFloat(rawData, ObjectDataParser.SKEW_Y, 0f) * Transform.DEG_RAD)
            transform.skew = Transform.normalizeRadian(
                getFloat(
                    rawData,
                    ObjectDataParser.SKEW_X,
                    0f
                ) * Transform.DEG_RAD
            ) - transform.rotation
        }

        transform.scaleX = getFloat(rawData, ObjectDataParser.SCALE_X, 1f)
        transform.scaleY = getFloat(rawData, ObjectDataParser.SCALE_Y, 1f)
    }

    /**
     * @private
     */
    protected fun _parseColorTransform(rawData: Any?, color: ColorTransform?) {
        color!!.alphaMultiplier = getFloat(rawData, ObjectDataParser.ALPHA_MULTIPLIER, 100f) * 0.01f
        color.redMultiplier = getFloat(rawData, ObjectDataParser.RED_MULTIPLIER, 100f) * 0.01f
        color.greenMultiplier = getFloat(rawData, ObjectDataParser.GREEN_MULTIPLIER, 100f) * 0.01f
        color.blueMultiplier = getFloat(rawData, ObjectDataParser.BLUE_MULTIPLIER, 100f) * 0.01f
        color.alphaOffset = getInt(rawData, ObjectDataParser.ALPHA_OFFSET, 0)
        color.redOffset = getInt(rawData, ObjectDataParser.RED_OFFSET, 0)
        color.greenOffset = getInt(rawData, ObjectDataParser.GREEN_OFFSET, 0)
        color.blueOffset = getInt(rawData, ObjectDataParser.BLUE_OFFSET, 0)
    }

    /**
     * @private
     */
    protected open fun _parseArray(rawData: Any) {
        this._data!!.intArray = ShortArray()
        this._data!!.floatArray = FloatArray()
        this._data!!.frameIntArray = ShortArray()
        this._data!!.frameFloatArray = FloatArray()
        this._data!!.frameArray = ShortArray()
        this._data!!.timelineArray = CharArray()
    }

    open fun parseDragonBonesDataInstance(rawData: Any): DragonBonesData? {
        return parseDragonBonesData(rawData, 1f)
    }

    /**
     * @inheritDoc
     */
    override fun parseDragonBonesData(rawData: Any, scale: Float): DragonBonesData? {
        val version = getString(rawData, ObjectDataParser.VERSION, "")
        val compatibleVersion = getString(rawData, ObjectDataParser.COMPATIBLE_VERSION, "")

        if (Arrays.asList<String>(*ObjectDataParser.DATA_VERSIONS).indexOf(version) >= 0 || Arrays.asList<String>(*ObjectDataParser.DATA_VERSIONS).indexOf(
                compatibleVersion
            ) >= 0
        ) {
            val data = BaseObject.borrowObject(DragonBonesData::class.java)
            data.version = version
            data.name = getString(rawData, ObjectDataParser.NAME, "")
            data.frameRate = getFloat(rawData, ObjectDataParser.FRAME_RATE, 24f)

            if (data.frameRate == 0f) { // Data error.
                data.frameRate = 24f
            }

            if (`in`(rawData, ObjectDataParser.ARMATURE)) {
                this._defalultColorOffset = -1
                this._data = data

                this._parseArray(rawData)

                val rawArmatures = getArray<Any>(rawData, ObjectDataParser.ARMATURE)
                for (rawArmature in rawArmatures!!) {
                    data.addArmature(this._parseArmature(rawArmature, scale))
                }

                if (this._intArrayJson.length() > 0) {
                    //this._parseWASMArray();
                    throw RuntimeException("this._parseWASMArray() not ported")
                }

                this._data = null
            }

            this._rawTextureAtlasIndex = 0
            if (`in`(rawData, ObjectDataParser.TEXTURE_ATLAS)) {
                this._rawTextureAtlases = getArray<Any>(rawData, ObjectDataParser.TEXTURE_ATLAS)
            } else {
                this._rawTextureAtlases = null
            }

            return data
        } else {
            Console._assert(false, "Nonsupport data version.")
        }

        return null
    }

    fun parseTextureAtlasData(rawData: Any, textureAtlasData: TextureAtlasData): Boolean {
        return parseTextureAtlasData(rawData, textureAtlasData, 0f)
    }

    /**
     * @inheritDoc
     */
    override fun parseTextureAtlasData(rawData: Any?, textureAtlasData: TextureAtlasData, scale: Float): Boolean {
        var scale = scale
        if (rawData == null) {
            if (this._rawTextureAtlases == null) {
                return false
            }

            val rawTextureAtlas = this._rawTextureAtlases!![this._rawTextureAtlasIndex++]
            this.parseTextureAtlasData(rawTextureAtlas, textureAtlasData, scale)
            if (this._rawTextureAtlasIndex >= this._rawTextureAtlases!!.length()) {
                this._rawTextureAtlasIndex = 0
                this._rawTextureAtlases = null
            }

            return true
        }

        // Texture format.
        textureAtlasData.width = getInt(rawData, ObjectDataParser.WIDTH, 0)
        textureAtlasData.height = getInt(rawData, ObjectDataParser.HEIGHT, 0)
        textureAtlasData.name = getString(rawData, ObjectDataParser.NAME, "")
        textureAtlasData.imagePath = getString(rawData, ObjectDataParser.IMAGE_PATH, "")

        if (scale > 0f) { // Use params scale.
            textureAtlasData.scale = scale
        } else { // Use data scale.
            textureAtlasData.scale = getFloat(rawData, ObjectDataParser.SCALE, textureAtlasData.scale)
            scale = textureAtlasData.scale
        }

        scale = 1f / scale //

        if (`in`(rawData, ObjectDataParser.SUB_TEXTURE)) {
            val rawTextures = getArray<Any>(rawData, ObjectDataParser.SUB_TEXTURE)
            var i = 0
            val l = rawTextures!!.length()
            while (i < l) {
                val rawTexture = rawTextures.getObject(i)
                val textureData = textureAtlasData.createTexture()
                textureData.rotated = getBool(rawTexture, ObjectDataParser.ROTATED, false)
                textureData.name = getString(rawTexture, ObjectDataParser.NAME, "")
                textureData.region.x = getFloat(rawTexture, ObjectDataParser.X, 0f) * scale
                textureData.region.y = getFloat(rawTexture, ObjectDataParser.Y, 0f) * scale
                textureData.region.width = getFloat(rawTexture, ObjectDataParser.WIDTH, 0f) * scale
                textureData.region.height = getFloat(rawTexture, ObjectDataParser.HEIGHT, 0f) * scale

                val frameWidth = getFloat(rawTexture, ObjectDataParser.FRAME_WIDTH, -1f)
                val frameHeight = getFloat(rawTexture, ObjectDataParser.FRAME_HEIGHT, -1f)
                if (frameWidth > 0f && frameHeight > 0f) {
                    textureData.frame = TextureData.createRectangle()
                    textureData.frame!!.x = getFloat(rawTexture, ObjectDataParser.FRAME_X, 0f) * scale
                    textureData.frame!!.y = getFloat(rawTexture, ObjectDataParser.FRAME_Y, 0f) * scale
                    textureData.frame!!.width = frameWidth * scale
                    textureData.frame!!.height = frameHeight * scale
                }

                textureAtlasData.addTexture(textureData)
                ++i
            }
        }

        return true
    }

    companion object {

        /**
         * @private
         */
        private var _objectDataParserInstance: ObjectDataParser? = null

        /**
         * @see BaseFactory.parseDragonBonesData
         */
        val instance: ObjectDataParser
            @Deprecated("已废弃，请参考 @see")
            get() {
                if (ObjectDataParser._objectDataParserInstance == null) {
                    ObjectDataParser._objectDataParserInstance = ObjectDataParser()
                }

                return ObjectDataParser._objectDataParserInstance
            }
    }
}
