package com.dragonbones.armature

import com.dragonbones.animation.AnimationState
import com.dragonbones.core.BinaryOffset
import com.dragonbones.core.BlendMode
import com.dragonbones.core.DisplayType
import com.dragonbones.geom.ColorTransform
import com.dragonbones.geom.Matrix
import com.dragonbones.geom.Point
import com.dragonbones.geom.Rectangle
import com.dragonbones.model.*
import com.dragonbones.util.Array
import com.dragonbones.util.FloatArray
import com.dragonbones.util.IntArray

/**
 * 插槽，附着在骨骼上，控制显示对象的显示状态和属性。
 * 一个骨骼上可以包含多个插槽。
 * 一个插槽中可以包含多个显示对象，同一时间只能显示其中的一个显示对象，但可以在动画播放的过程中切换显示对象实现帧动画。
 * 显示对象可以是普通的图片纹理，也可以是子骨架的显示容器，网格显示对象，还可以是自定义的其他显示对象。
 *
 * @version DragonBones 3.0
 * @language zh_CN
 * @see Armature
 *
 * @see Bone
 *
 * @see SlotData
 */
abstract class Slot : TransformObject() {
    /**
     * 显示对象受到控制的动画状态或混合组名称，设置为 null 则表示受所有的动画状态控制。
     *
     * @default null
     * @version DragonBones 4.5
     * @language zh_CN
     * @see AnimationState.displayControl
     *
     * @see AnimationState.name
     *
     * @see AnimationState.group
     */
    var displayController: String? = null
    /**
     * @readonly
     */
    var slotData: SlotData? = null
    /**
     * @private
     */
    protected var _displayDirty: Boolean = false
    /**
     * @private
     */
    protected var _zOrderDirty: Boolean = false
    /**
     * @private
     */
    protected var _visibleDirty: Boolean = false
    /**
     * @private
     */
    protected var _blendModeDirty: Boolean = false
    /**
     * @private
     */
    var _colorDirty: Boolean = false
    /**
     * @private
     */
    var _meshDirty: Boolean = false
    /**
     * @private
     */
    protected var _transformDirty: Boolean = false
    /**
     * @private
     */
    protected var _visible: Boolean = false
    /**
     * @private
     */
    protected var _blendMode: BlendMode = BlendMode.Normal
    /**
     * @private
     */
    protected var _displayIndex: Int = 0
    /**
     * @private
     */
    protected var _animationDisplayIndex: Float = 0.toFloat()
    /**
     * @private
     */
    var _zOrder: Float = 0.toFloat()
    /**
     * @private
     */
    protected var _cachedFrameIndex: Int = 0
    /**
     * @private
     */
    var _pivotX: Float = 0.toFloat()
    /**
     * @private
     */
    var _pivotY: Float = 0.toFloat()
    /**
     * @private
     */
    protected val _localMatrix = Matrix()
    /**
     * @private
     */
    val _colorTransform = ColorTransform()
    /**
     * @private
     */
    val _ffdVertices = FloatArray()
    /**
     * @private
     */
    val _displayDatas = Array<DisplayData>()
    /**
     * @private
     */
    // ArrayList<Armature | any>
    protected val _displayList = Array<Any>()
    /**
     * @private
     */
    protected val _meshBones = Array<Bone>()
    /**
     * @internal
     * @private
     */
    var _rawDisplayDatas: Array<DisplayData>? = Array()
    /**
     * @private
     */
    protected var _displayData: DisplayData? = null
    /**
     * @private
     */
    protected var _textureData: TextureData? = null
    /**
     * @private
     */
    var _meshData: MeshDisplayData? = null
    /**
     * @private
     */
    /**
     * @language zh_CN
     * 插槽此时的自定义包围盒数据。
     * @version DragonBones 3.0
     * @see Armature
     */
    var boundingBoxData: BoundingBoxData? = null
        protected set
    /**
     * @private
     */
    /**
     * @private
     */
    var rawDisplay: Any? = null
        protected set // Initial value.
    /**
     * @private
     */
    /**
     * @private
     */
    var meshDisplay: Any? = null
        protected set // Initial value.
    /**
     * @private
     */
    protected var _display: Any? = null
    /**
     * @private
     */
    protected var _childArmature: Armature? = null
    /**
     * @internal
     * @private
     */
    var _cachedFrameIndices: IntArray? = null

    /**
     * 此时显示的显示对象在显示列表中的索引。
     *
     * @version DragonBones 4.5
     * @language zh_CN
     */
    var displayIndex: Int
        get() = this._displayIndex
        set(value) {
            if (this._setDisplayIndex(value)) {
                this.update(-1)
            }
        }

    /**
     * 包含显示对象或子骨架的显示列表。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    // Copy.
    // Release replaced displays.
    var displayList: Array<Any>
        get() = this._displayList.copy()
        set(value) {
            val backupDisplayList = this._displayList.copy()
            val disposeDisplayList = Array<Any>()

            if (this._setDisplayList(value)) {
                this.update(-1)
            }
            for (eachDisplay in backupDisplayList) {
                if (eachDisplay != null && eachDisplay !== this.rawDisplay && eachDisplay !== this.meshDisplay &&
                    this._displayList.indexOf(eachDisplay) < 0 &&
                    disposeDisplayList.indexOf(eachDisplay) < 0
                ) {
                    disposeDisplayList.add(eachDisplay)
                }
            }

            for (eachDisplay in disposeDisplayList) {
                if (eachDisplay is Armature) {
                    eachDisplay.dispose()
                } else {
                    this._disposeDisplay(eachDisplay)
                }
            }
        }

    /**
     * 此时显示的子骨架。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     * @see Armature
     */
    var childArmature: Armature?
        get() = this._childArmature
        set(value) {
            if (this._childArmature === value) {
                return
            }

            this.display = value
        }

    /**
     * 此时显示的显示对象。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    //@Override
    // Emprty.
    // Copy.
    var display: Any?
        get() = this._display
        set(value) {
            if (this._display === value) {
                return
            }

            val displayListLength = this._displayList.size()
            if (this._displayIndex < 0 && displayListLength == 0) {
                this._displayIndex = 0
            }

            if (this._displayIndex < 0) {
                return
            } else {
                val replaceDisplayList = this.displayList
                if (displayListLength <= this._displayIndex) {
                    replaceDisplayList.length = this._displayIndex + 1
                }

                replaceDisplayList.set(this._displayIndex, value)
                this.displayList = replaceDisplayList
            }
        }

    /**
     * @private
     */
    override fun _onClear() {
        super._onClear()

        val disposeDisplayList = Array<Any>()
        for (eachDisplay in this._displayList) {
            if (eachDisplay != null && eachDisplay !== this.rawDisplay && eachDisplay !== this.meshDisplay &&
                disposeDisplayList.indexOf(eachDisplay) < 0
            ) {
                disposeDisplayList.add(eachDisplay)
            }
        }

        for (eachDisplay in disposeDisplayList) {
            if (eachDisplay is Armature) {
                eachDisplay.dispose()
            } else {
                this._disposeDisplay(eachDisplay)
            }
        }

        if (this.meshDisplay != null && this.meshDisplay !== this.rawDisplay) { // May be _meshDisplay and _rawDisplay is the same one.
            this._disposeDisplay(this.meshDisplay)
        }

        if (this.rawDisplay != null) {
            this._disposeDisplay(this.rawDisplay)
        }

        this.displayController = null
        this.slotData = null //

        this._displayDirty = false
        this._zOrderDirty = false
        this._blendModeDirty = false
        this._colorDirty = false
        this._meshDirty = false
        this._transformDirty = false
        this._visible = true
        this._blendMode = BlendMode.Normal
        this._displayIndex = -1
        this._animationDisplayIndex = -1f
        this._zOrder = 0f
        this._cachedFrameIndex = -1
        this._pivotX = 0f
        this._pivotY = 0f
        this._localMatrix.identity()
        this._colorTransform.identity()
        this._ffdVertices.clear()
        this._displayList.clear()
        this._displayDatas.clear()
        this._meshBones.clear()
        this._rawDisplayDatas = null //
        this._displayData = null
        this._textureData = null
        this._meshData = null
        this.boundingBoxData = null
        this.rawDisplay = null
        this.meshDisplay = null
        this._display = null
        this._childArmature = null
        this._cachedFrameIndices = null
    }

    /**
     * @private
     */
    protected abstract fun _initDisplay(value: Any)

    /**
     * @private
     */
    protected abstract fun _disposeDisplay(value: Any?)

    /**
     * @private
     */
    protected abstract fun _onUpdateDisplay()

    /**
     * @private
     */
    protected abstract fun _addDisplay()

    /**
     * @private
     */
    protected abstract fun _replaceDisplay(value: Any)

    /**
     * @private
     */
    protected abstract fun _removeDisplay()

    /**
     * @private
     */
    protected abstract fun _updateZOrder()

    /**
     * @private
     */
    abstract fun _updateVisible()

    /**
     * @private
     */
    protected abstract fun _updateBlendMode()

    /**
     * @private
     */
    protected abstract fun _updateColor()

    /**
     * @private
     */
    protected abstract fun _updateFrame()

    /**
     * @private
     */
    protected abstract fun _updateMesh()

    /**
     * @private
     */
    protected abstract fun _updateTransform(isSkinnedMesh: Boolean)

    /**
     * @private
     */
    protected fun _updateDisplayData() {
        val prevDisplayData = this._displayData
        val prevTextureData = this._textureData
        val prevMeshData = this._meshData
        val rawDisplayData =
            if (this._displayIndex >= 0 && this._displayIndex < this._rawDisplayDatas!!.size()) this._rawDisplayDatas!!.get(
                this._displayIndex
            ) else null

        if (this._displayIndex >= 0 && this._displayIndex < this._displayDatas.size()) {
            this._displayData = this._displayDatas.get(this._displayIndex)
        } else {
            this._displayData = null
        }

        // Update texture and mesh data.
        if (this._displayData != null) {
            if (this._displayData!!.type == DisplayType.Image || this._displayData!!.type == DisplayType.Mesh) {
                this._textureData = (this._displayData as ImageDisplayData).texture
                if (this._displayData!!.type == DisplayType.Mesh) {
                    this._meshData = this._displayData as MeshDisplayData?
                } else if (rawDisplayData != null && rawDisplayData.type == DisplayType.Mesh) {
                    this._meshData = rawDisplayData as MeshDisplayData?
                } else {
                    this._meshData = null
                }
            } else {
                this._textureData = null
                this._meshData = null
            }
        } else {
            this._textureData = null
            this._meshData = null
        }

        // Update bounding box data.
        if (this._displayData != null && this._displayData!!.type == DisplayType.BoundingBox) {
            this.boundingBoxData = (this._displayData as BoundingBoxDisplayData).boundingBox
        } else if (rawDisplayData != null && rawDisplayData.type == DisplayType.BoundingBox) {
            this.boundingBoxData = (rawDisplayData as BoundingBoxDisplayData).boundingBox
        } else {
            this.boundingBoxData = null
        }

        if (this._displayData !== prevDisplayData || this._textureData !== prevTextureData || this._meshData !== prevMeshData) {
            // Update pivot offset.
            if (this._meshData != null) {
                this._pivotX = 0f
                this._pivotY = 0f
            } else if (this._textureData != null) {
                val imageDisplayData = this._displayData as ImageDisplayData?
                val scale = this.armature!!.armatureData!!.scale
                val frame = this._textureData!!.frame

                this._pivotX = imageDisplayData!!.pivot.x
                this._pivotY = imageDisplayData.pivot.y

                val rect = frame ?: this._textureData!!.region
                var width = rect.width * scale
                var height = rect.height * scale

                if (this._textureData!!.rotated && frame == null) {
                    width = rect.height
                    height = rect.width
                }

                this._pivotX *= width
                this._pivotY *= height

                if (frame != null) {
                    this._pivotX += frame.x * scale
                    this._pivotY += frame.y * scale
                }
            } else {
                this._pivotX = 0f
                this._pivotY = 0f
            }

            // Update mesh bones and ffd vertices.
            if (this._meshData !== prevMeshData) {
                if (this._meshData != null) { // && this._meshData == this._displayData
                    if (this._meshData!!.weight != null) {
                        this._ffdVertices!!.length = this._meshData!!.weight!!.count * 2
                        this._meshBones.length = this._meshData!!.weight!!.bones.size()

                        var i = 0
                        val l = this._meshBones.size()
                        while (i < l) {
                            this._meshBones.set(i, this.armature!!.getBone(this._meshData!!.weight!!.bones.get(i).name))
                            ++i
                        }
                    } else {
                        val vertexCount =
                            this._meshData!!.parent!!.parent!!.intArray!!.get(this._meshData!!.offset + BinaryOffset.MeshVertexCount.v)
                        this._ffdVertices.length = vertexCount * 2
                        this._meshBones.clear()
                    }

                    var i = 0
                    val l = this._ffdVertices.size()
                    while (i < l) {
                        this._ffdVertices.set(i, 0f)
                        ++i
                    }

                    this._meshDirty = true
                } else {
                    this._ffdVertices.clear()
                    this._meshBones.clear()
                }
            } else if (this._meshData != null && this._textureData !== prevTextureData) { // Update mesh after update frame.
                this._meshDirty = true
            }

            if (this._displayData != null && rawDisplayData != null && this._displayData !== rawDisplayData && this._meshData == null) {
                rawDisplayData.transform.toMatrix(_helpMatrix)
                _helpMatrix.invert()
                _helpMatrix.transformPoint(0f, 0f, _helpPoint)
                this._pivotX -= _helpPoint.x
                this._pivotY -= _helpPoint.y

                this._displayData!!.transform.toMatrix(_helpMatrix)
                _helpMatrix.invert()
                _helpMatrix.transformPoint(0f, 0f, _helpPoint)
                this._pivotX += _helpPoint.x
                this._pivotY += _helpPoint.y
            }

            // Update original transform.
            if (rawDisplayData != null) {
                this.origin = rawDisplayData.transform
            } else if (this._displayData != null) {
                this.origin = this._displayData!!.transform
            }

            this._displayDirty = true
            this._transformDirty = true
        }
    }

    /**
     * @private
     */
    protected fun _updateDisplay() {
        val prevDisplay = if (this._display != null) this._display else this.rawDisplay
        val prevChildArmature = this._childArmature

        // Update display and child armature.
        if (this._displayIndex >= 0 && this._displayIndex < this._displayList.size()) {
            this._display = this._displayList.get(this._displayIndex)
            if (this._display != null && this._display is Armature) {
                this._childArmature = this._display as Armature?
                this._display = this._childArmature!!.display
            } else {
                this._childArmature = null
            }
        } else {
            this._display = null
            this._childArmature = null
        }

        // Update display.
        val currentDisplay = if (this._display != null) this._display else this.rawDisplay
        if (currentDisplay !== prevDisplay) {
            this._onUpdateDisplay()
            this._replaceDisplay(prevDisplay!!)

            this._visibleDirty = true
            this._blendModeDirty = true
            this._colorDirty = true
        }

        // Update frame.
        if (currentDisplay === this.rawDisplay || currentDisplay === this.meshDisplay) {
            this._updateFrame()
        }

        // Update child armature.
        if (this._childArmature !== prevChildArmature) {
            if (prevChildArmature != null) {
                prevChildArmature.parent = null // Update child armature parent.
                prevChildArmature.clock = null
                if (prevChildArmature.inheritAnimation) {
                    prevChildArmature.animation!!.reset()
                }
            }

            if (this._childArmature != null) {
                this._childArmature!!.parent = this // Update child armature parent.
                this._childArmature!!.clock = this.armature!!.clock
                if (this._childArmature!!.inheritAnimation) { // Set child armature cache frameRate.
                    if (this._childArmature!!.cacheFrameRate == 0f) {
                        val cacheFrameRate = this.armature!!.cacheFrameRate
                        if (cacheFrameRate != 0f) {
                            this._childArmature!!.cacheFrameRate = cacheFrameRate
                        }
                    }

                    // Child armature action.
                    var actions: Array<ActionData>? = null
                    if (this._displayData != null && this._displayData!!.type == DisplayType.Armature) {
                        actions = (this._displayData as ArmatureDisplayData).actions
                    } else {
                        val rawDisplayData =
                            if (this._displayIndex >= 0 && this._displayIndex < this._rawDisplayDatas!!.size()) this._rawDisplayDatas!!.get(
                                this._displayIndex
                            ) else null
                        if (rawDisplayData != null && rawDisplayData.type == DisplayType.Armature) {
                            actions = (rawDisplayData as ArmatureDisplayData).actions
                        }
                    }

                    if (actions != null && actions.size() > 0) {
                        for (action in actions) {
                            this._childArmature!!._bufferAction(
                                action,
                                false
                            ) // Make sure default action at the beginning.
                        }
                    } else {
                        this._childArmature!!.animation!!.play()
                    }
                }
            }
        }
    }

    /**
     * @private
     */
    protected fun _updateGlobalTransformMatrix(isCache: Boolean) {
        this.globalTransformMatrix.copyFrom(this._localMatrix)
        this.globalTransformMatrix.concat(this.parent!!.globalTransformMatrix)
        if (isCache) {
            this.global.fromMatrix(this.globalTransformMatrix)
        } else {
            this._globalDirty = true
        }
    }

    /**
     * @private
     */
    protected fun _isMeshBonesUpdate(): Boolean {
        for (bone in this._meshBones) {
            if (bone != null && bone._childrenTransformDirty) {
                return true
            }
        }

        return false
    }

    /**
     * @internal
     * @private
     */
    override fun _setArmature(value: Armature?) {
        if (this.armature === value) {
            return
        }

        if (this.armature != null) {
            this.armature!!._removeSlotFromSlotList(this)
        }

        this.armature = value //

        this._onUpdateDisplay()

        if (this.armature != null) {
            this.armature!!._addSlotToSlotList(this)
            this._addDisplay()
        } else {
            this._removeDisplay()
        }
    }

    /**
     * @internal
     * @private
     */
    @JvmOverloads
    fun _setDisplayIndex(value: Int, isAnimation: Boolean = false): Boolean {
        if (isAnimation) {
            if (this._animationDisplayIndex == value.toFloat()) {
                return false
            }

            this._animationDisplayIndex = value.toFloat()
        }

        if (this._displayIndex == value) {
            return false
        }

        this._displayIndex = value
        this._displayDirty = true

        this._updateDisplayData()

        return this._displayDirty
    }

    /**
     * @internal
     * @private
     */
    fun _setZorder(value: Float): Boolean {
        if (this._zOrder == value) {
            //return false;
        }

        this._zOrder = value
        this._zOrderDirty = true

        return this._zOrderDirty
    }

    /**
     * @internal
     * @private
     */
    fun _setColor(value: ColorTransform): Boolean {
        this._colorTransform.copyFrom(value)
        this._colorDirty = true

        return this._colorDirty
    }

    /**
     * @private
     */
    fun _setDisplayList(value: Array<Any>?): Boolean {
        if (value != null && value.size() > 0) {
            if (this._displayList.size() != value.size()) {
                this._displayList.length = value.size()
            }

            var i = 0
            val l = value.size()
            while (i < l) { // Retain input render displays.
                val eachDisplay = value.get(i)
                if (eachDisplay != null && eachDisplay !== this.rawDisplay && eachDisplay !== this.meshDisplay &&
                    eachDisplay !is Armature && this._displayList.indexOf(eachDisplay) < 0
                ) {
                    this._initDisplay(eachDisplay)
                }

                this._displayList.set(i, eachDisplay)
                ++i
            }
        } else if (this._displayList.size() > 0) {
            this._displayList.clear()
        }

        if (this._displayIndex >= 0 && this._displayIndex < this._displayList.size()) {
            this._displayDirty = this._display !== this._displayList.get(this._displayIndex)
        } else {
            this._displayDirty = this._display != null
        }

        this._updateDisplayData()

        return this._displayDirty
    }

    /**
     * @private
     */
    fun init(slotData: SlotData, displayDatas: Array<DisplayData>?, rawDisplay: Any, meshDisplay: Any) {
        if (this.slotData != null) {
            return
        }

        this.slotData = slotData
        this.name = this.slotData!!.name

        this._visibleDirty = true
        this._blendModeDirty = true
        this._colorDirty = true
        this._blendMode = this.slotData!!.blendMode
        this._zOrder = this.slotData!!.zOrder
        this._colorTransform.copyFrom(this.slotData!!.color!!)
        this._rawDisplayDatas = displayDatas
        this.rawDisplay = rawDisplay
        this.meshDisplay = meshDisplay

        this._displayDatas.length = this._rawDisplayDatas!!.size()
        var i = 0
        val l = this._displayDatas.size()
        while (i < l) {
            this._displayDatas.set(i, this._rawDisplayDatas!!.get(i))
            ++i
        }
    }

    /**
     * @internal
     * @private
     */
    fun update(cacheFrameIndex: Int) {
        var cacheFrameIndex = cacheFrameIndex
        if (this._displayDirty) {
            this._displayDirty = false
            this._updateDisplay()

            if (this._transformDirty) { // Update local matrix. (Only updated when both display and transform are dirty.)
                if (this.origin != null) {
                    this.global.copyFrom(this.origin!!).add(this.offset).toMatrix(this._localMatrix)
                } else {
                    this.global.copyFrom(this.offset).toMatrix(this._localMatrix)
                }
            }
        }

        if (this._zOrderDirty) {
            this._zOrderDirty = false
            this._updateZOrder()
        }

        if (cacheFrameIndex >= 0 && this._cachedFrameIndices != null) {
            val cachedFrameIndex = this._cachedFrameIndices!!.get(cacheFrameIndex)
            if (cachedFrameIndex >= 0 && this._cachedFrameIndex == cachedFrameIndex) { // Same cache.
                this._transformDirty = false
            } else if (cachedFrameIndex >= 0) { // Has been Cached.
                this._transformDirty = true
                this._cachedFrameIndex = cachedFrameIndex
            } else if (this._transformDirty || this.parent!!._childrenTransformDirty) { // Dirty.
                this._transformDirty = true
                this._cachedFrameIndex = -1
            } else if (this._cachedFrameIndex >= 0) { // Same cache, but not set index yet.
                this._transformDirty = false
                this._cachedFrameIndices!!.set(cacheFrameIndex, this._cachedFrameIndex)
            } else { // Dirty.
                this._transformDirty = true
                this._cachedFrameIndex = -1
            }
        } else if (this._transformDirty || this.parent!!._childrenTransformDirty) { // Dirty.
            cacheFrameIndex = -1
            this._transformDirty = true
            this._cachedFrameIndex = -1
        }

        if (this._display == null) {
            return
        }

        if (this._blendModeDirty) {
            this._blendModeDirty = false
            this._updateBlendMode()
        }

        if (this._colorDirty) {
            this._colorDirty = false
            this._updateColor()
        }

        if (this._meshData != null && this._display === this.meshDisplay) {
            val isSkinned = this._meshData!!.weight != null
            if (this._meshDirty || isSkinned && this._isMeshBonesUpdate()) {
                this._meshDirty = false
                this._updateMesh()
            }

            if (isSkinned) {
                if (this._transformDirty) {
                    this._transformDirty = false
                    this._updateTransform(true)
                }

                return
            }
        }

        if (this._transformDirty) {
            this._transformDirty = false

            if (this._cachedFrameIndex < 0) {
                val isCache = cacheFrameIndex >= 0
                this._updateGlobalTransformMatrix(isCache)

                if (isCache && this._cachedFrameIndices != null) {
                    val vv = this.armature!!.armatureData!!.setCacheFrame(this.globalTransformMatrix, this.global)
                    this._cachedFrameIndices!!.set(cacheFrameIndex, vv)
                    this._cachedFrameIndex = vv
                }
            } else {
                this.armature!!.armatureData!!.getCacheFrame(
                    this.globalTransformMatrix,
                    this.global,
                    this._cachedFrameIndex
                )
            }

            this._updateTransform(false)
        }
    }

    /**
     * @private
     */
    fun updateTransformAndMatrix() {
        if (this._transformDirty) {
            this._transformDirty = false
            this._updateGlobalTransformMatrix(false)
        }
    }

    /**
     * 判断指定的点是否在插槽的自定义包围盒内。
     *
     * @param x 点的水平坐标。（骨架内坐标系）
     * @param y 点的垂直坐标。（骨架内坐标系）
     * @version DragonBones 5.0
     * @language zh_CN
     */
    fun containsPoint(x: Float, y: Float): Boolean {
        if (this.boundingBoxData == null) {
            return false
        }

        this.updateTransformAndMatrix()

        _helpMatrix.copyFrom(this.globalTransformMatrix)
        _helpMatrix.invert()
        _helpMatrix.transformPoint(x, y, _helpPoint)

        return this.boundingBoxData!!.containsPoint(_helpPoint.x, _helpPoint.y)
    }

    fun intersectsSegment(
        xA: Float, yA: Float, xB: Float, yB: Float
    ): Float {
        return intersectsSegment(xA, yA, xB, yB, null, null, null).toFloat()
    }

    /**
     * 判断指定的线段与插槽的自定义包围盒是否相交。
     *
     * @param xA                 线段起点的水平坐标。（骨架内坐标系）
     * @param yA                 线段起点的垂直坐标。（骨架内坐标系）
     * @param xB                 线段终点的水平坐标。（骨架内坐标系）
     * @param yB                 线段终点的垂直坐标。（骨架内坐标系）
     * @param intersectionPointA 线段从起点到终点与包围盒相交的第一个交点。（骨架内坐标系）
     * @param intersectionPointB 线段从终点到起点与包围盒相交的第一个交点。（骨架内坐标系）
     * @param normalRadians      碰撞点处包围盒切线的法线弧度。 [x: 第一个碰撞点处切线的法线弧度, y: 第二个碰撞点处切线的法线弧度]
     * @returns 相交的情况。 [-1: 不相交且线段在包围盒内, 0: 不相交, 1: 相交且有一个交点且终点在包围盒内, 2: 相交且有一个交点且起点在包围盒内, 3: 相交且有两个交点, N: 相交且有 N 个交点]
     * @version DragonBones 5.0
     * @language zh_CN
     */
    fun intersectsSegment(
        xA: Float, yA: Float, xB: Float, yB: Float,
        intersectionPointA: Point?,
        intersectionPointB: Point?,
        normalRadians: Point?
    ): Int {
        var xA = xA
        var yA = yA
        var xB = xB
        var yB = yB
        if (this.boundingBoxData == null) {
            return 0
        }

        this.updateTransformAndMatrix()
        _helpMatrix.copyFrom(this.globalTransformMatrix)
        _helpMatrix.invert()
        _helpMatrix.transformPoint(xA, yA, _helpPoint)
        xA = _helpPoint.x
        yA = _helpPoint.y
        _helpMatrix.transformPoint(xB, yB, _helpPoint)
        xB = _helpPoint.x
        yB = _helpPoint.y

        val intersectionCount = this.boundingBoxData!!.intersectsSegment(
            xA,
            yA,
            xB,
            yB,
            intersectionPointA,
            intersectionPointB,
            normalRadians
        )
        if (intersectionCount > 0) {
            if (intersectionCount == 1 || intersectionCount == 2) {
                if (intersectionPointA != null) {
                    this.globalTransformMatrix.transformPoint(
                        intersectionPointA.x,
                        intersectionPointA.y,
                        intersectionPointA
                    )
                    if (intersectionPointB != null) {
                        intersectionPointB.x = intersectionPointA.x
                        intersectionPointB.y = intersectionPointA.y
                    }
                } else if (intersectionPointB != null) {
                    this.globalTransformMatrix.transformPoint(
                        intersectionPointB.x,
                        intersectionPointB.y,
                        intersectionPointB
                    )
                }
            } else {
                if (intersectionPointA != null) {
                    this.globalTransformMatrix.transformPoint(
                        intersectionPointA.x,
                        intersectionPointA.y,
                        intersectionPointA
                    )
                }

                if (intersectionPointB != null) {
                    this.globalTransformMatrix.transformPoint(
                        intersectionPointB.x,
                        intersectionPointB.y,
                        intersectionPointB
                    )
                }
            }

            if (normalRadians != null) {
                this.globalTransformMatrix.transformPoint(
                    Math.cos(normalRadians.x.toDouble()).toFloat(),
                    Math.sin(normalRadians.x.toDouble()).toFloat(),
                    _helpPoint,
                    true
                )
                normalRadians.x = Math.atan2(_helpPoint.y.toDouble(), _helpPoint.x.toDouble()).toFloat()

                this.globalTransformMatrix.transformPoint(
                    Math.cos(normalRadians.y.toDouble()).toFloat(),
                    Math.sin(normalRadians.y.toDouble()).toFloat(),
                    _helpPoint,
                    true
                )
                normalRadians.y = Math.atan2(_helpPoint.y.toDouble(), _helpPoint.x.toDouble()).toFloat()
            }
        }

        return intersectionCount
    }

    /**
     * 在下一帧更新显示对象的状态。
     *
     * @version DragonBones 4.5
     * @language zh_CN
     */
    fun invalidUpdate() {
        this._displayDirty = true
        this._transformDirty = true
    }

    ///**
    // * @see #display
    // * @deprecated 已废弃，请参考 @see
    // */
    //public Object getDisplay() {
    //    return this._display;
    //}

    ///**
    // * @see #display
    // * @deprecated 已废弃，请参考 @see
    // */
    //public void setDisplay(Object value) {
    //   this._display = value;
    //
}
