package com.dragonbones.armature

import com.dragonbones.core.DragonBones
import com.dragonbones.core.OffsetMode
import com.dragonbones.geom.Matrix
import com.dragonbones.geom.Transform
import com.dragonbones.model.BoneData
import com.dragonbones.util.Array
import com.dragonbones.util.IntArray

/**
 * 骨骼，一个骨架中可以包含多个骨骼，骨骼以树状结构组成骨架。
 * 骨骼在骨骼动画体系中是最重要的逻辑单元之一，负责动画中的平移旋转缩放的实现。
 *
 * @version DragonBones 3.0
 * @language zh_CN
 * @see BoneData
 *
 * @see Armature
 *
 * @see Slot
 */
class Bone : TransformObject() {
    /**
     * @private
     */
    var offsetMode: OffsetMode = OffsetMode.Additive
    /**
     * @internal
     * @private
     */
    val animationPose = Transform()
    /**
     * @internal
     * @private
     */
    val constraints = Array<Constraint>()
    /**
     * @readonly
     */
    var boneData: BoneData? = null
    /**
     * @internal
     * @private
     */
    var _transformDirty: Boolean = false
    /**
     * @internal
     * @private
     */
    var _childrenTransformDirty: Boolean = false
    /**
     * @internal
     * @private
     */
    var _blendDirty: Boolean = false
    private var _localDirty: Boolean = false
    private var _visible: Boolean = false
    private var _cachedFrameIndex: Int = 0
    /**
     * @internal
     * @private
     */
    var _blendLayer: Float = 0.toFloat()
    /**
     * @internal
     * @private
     */
    var _blendLeftWeight: Float = 0.toFloat()
    /**
     * @internal
     * @private
     */
    var _blendLayerWeight: Float = 0.toFloat()
    private val _bones = Array<Bone>()
    private val _slots = Array<Slot>()
    /**
     * @internal
     * @private
     */
    var _cachedFrameIndices: IntArray? = IntArray()

    /**
     * 所有的子骨骼。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    val bones: Array<Bone>
        get() {
            this._bones.clear()

            for (bone in this.armature!!.bones) {
                if (bone.parent === this) {
                    this._bones.add(bone)
                }
            }

            return this._bones
        }

    /**
     * 所有的插槽。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     * @see Slot
     */
    val slots: Array<Slot>
        get() {
            this._slots.clear()

            for (slot in this.armature!!.slots) {
                if (slot.parent === this) {
                    this._slots.add(slot)
                }
            }

            return this._slots
        }

    /**
     * 控制此骨骼所有插槽的可见。
     *
     * @default true
     * @version DragonBones 3.0
     * @language zh_CN
     * @see Slot
     */
    var visible: Boolean
        get() = this._visible
        set(value) {
            if (this._visible == value) {
                return
            }

            this._visible = value

            for (slot in this.armature!!.slots) {
                if (slot.parent === this) {
                    slot._updateVisible()
                }
            }
        }

    /**
     * @see .boneData
     *
     * @see BoneData.length
     *
     */
    val length: Float
        @Deprecated("已废弃，请参考 @see")
        get() = this.boneData!!.length

    /**
     * @see Armature.getSlot
     */
    val slot: Slot?
        @Deprecated("已废弃，请参考 @see")
        get() {
            for (slot in this.armature!!.slots) {
                if (slot.parent === this) {
                    return slot
                }
            }

            return null
        }

    /**
     * @private
     */
    override fun _onClear() {
        super._onClear()

        for (constraint in this.constraints) {
            constraint.returnToPool()
        }

        this.offsetMode = OffsetMode.Additive
        this.animationPose.identity()
        this.constraints.clear()
        this.boneData = null //

        this._transformDirty = false
        this._childrenTransformDirty = false
        this._blendDirty = false
        this._localDirty = true
        this._visible = true
        this._cachedFrameIndex = -1
        this._blendLayer = 0f
        this._blendLeftWeight = 1f
        this._blendLayerWeight = 0f
        this._bones.clear()
        this._slots.clear()
        this._cachedFrameIndices = null
    }

    /**
     * @private
     */
    private fun _updateGlobalTransformMatrix(isCache: Boolean) {
        val flipX = this.armature!!.flipX
        val flipY = this.armature!!.flipY == DragonBones.yDown
        val global = this.global
        val globalTransformMatrix = this.globalTransformMatrix
        var inherit = this.parent != null
        var dR = 0f

        if (this.offsetMode == OffsetMode.Additive) {
            // global.copyFrom(this.origin).add(this.offset).add(this.animationPose);
            global.x = this.origin!!.x + this.offset.x + this.animationPose.x
            global.y = this.origin!!.y + this.offset.y + this.animationPose.y
            global.skew = this.origin!!.skew + this.offset.skew + this.animationPose.skew
            global.rotation = this.origin!!.rotation + this.offset.rotation + this.animationPose.rotation
            global.scaleX = this.origin!!.scaleX * this.offset.scaleX * this.animationPose.scaleX
            global.scaleY = this.origin!!.scaleY * this.offset.scaleY * this.animationPose.scaleY
        } else if (this.offsetMode == OffsetMode.None) {
            global.copyFrom(this.origin!!).add(this.animationPose)
        } else {
            inherit = false
            global.copyFrom(this.offset)
        }

        if (inherit) {
            val parentMatrix = this.parent!!.globalTransformMatrix

            if (this.boneData!!.inheritScale) {
                if (!this.boneData!!.inheritRotation) {
                    this.parent!!.updateGlobalTransform()

                    dR = this.parent!!.global.rotation //
                    global.rotation -= dR
                }

                global.toMatrix(globalTransformMatrix)
                globalTransformMatrix.concat(parentMatrix)

                if (this.boneData!!.inheritTranslation) {
                    global.x = globalTransformMatrix.tx
                    global.y = globalTransformMatrix.ty
                } else {
                    globalTransformMatrix.tx = global.x
                    globalTransformMatrix.ty = global.y
                }

                if (isCache) {
                    global.fromMatrix(globalTransformMatrix)
                } else {
                    this._globalDirty = true
                }
            } else {
                if (this.boneData!!.inheritTranslation) {
                    val x = global.x
                    val y = global.y
                    global.x = parentMatrix.a * x + parentMatrix.c * y + parentMatrix.tx
                    global.y = parentMatrix.d * y + parentMatrix.b * x + parentMatrix.ty
                } else {
                    if (flipX) {
                        global.x = -global.x
                    }

                    if (flipY) {
                        global.y = -global.y
                    }
                }

                if (this.boneData!!.inheritRotation) {
                    this.parent!!.updateGlobalTransform()
                    dR = this.parent!!.global.rotation

                    if (this.parent!!.global.scaleX < 0f) {
                        dR += Math.PI.toFloat()
                    }

                    if (parentMatrix.a * parentMatrix.d - parentMatrix.b * parentMatrix.c < 0f) {
                        dR -= (global.rotation * 2.0).toFloat()

                        if (flipX != flipY || this.boneData!!.inheritReflection) {
                            global.skew += Math.PI.toFloat()
                        }
                    }

                    global.rotation += dR
                } else if (flipX || flipY) {
                    if (flipX && flipY) {
                        dR = Math.PI.toFloat()
                    } else {
                        dR = -global.rotation * 2.0f
                        if (flipX) {
                            dR += Math.PI.toFloat()
                        }

                        global.skew += Math.PI.toFloat()
                    }

                    global.rotation += dR
                }

                global.toMatrix(globalTransformMatrix)
            }
        } else {
            if (flipX || flipY) {
                if (flipX) {
                    global.x = -global.x
                }

                if (flipY) {
                    global.y = -global.y
                }

                if (flipX && flipY) {
                    dR = Math.PI.toFloat()
                } else {
                    dR = -global.rotation * 2.0f
                    if (flipX) {
                        dR += Math.PI.toFloat()
                    }

                    global.skew += Math.PI.toFloat()
                }

                global.rotation += dR
            }

            global.toMatrix(globalTransformMatrix)
        }
    }

    /**
     * @internal
     * @private
     */
    override fun _setArmature(value: Armature?) {
        if (this.armature === value) {
            return
        }

        var oldSlots: Array<Slot>? = null
        var oldBones: Array<Bone>? = null

        if (this.armature != null) {
            oldSlots = this.slots
            oldBones = this.bones
            this.armature!!._removeBoneFromBoneList(this)
        }

        this.armature = value //

        if (this.armature != null) {
            this.armature!!._addBoneToBoneList(this)
        }

        if (oldSlots != null) {
            for (slot in oldSlots) {
                if (slot.parent === this) {
                    slot._setArmature(this.armature)
                }
            }
        }

        if (oldBones != null) {
            for (bone in oldBones) {
                if (bone.parent === this) {
                    bone._setArmature(this.armature)
                }
            }
        }
    }

    /**
     * @internal
     * @private
     */
    fun init(boneData: BoneData) {
        if (this.boneData != null) {
            return
        }

        this.boneData = boneData
        this.name = this.boneData!!.name
        this.origin = this.boneData!!.transform
    }

    /**
     * @internal
     * @private
     */
    fun update(cacheFrameIndex: Int) {
        var cacheFrameIndex = cacheFrameIndex
        this._blendDirty = false

        if (cacheFrameIndex >= 0 && this._cachedFrameIndices != null) {
            val cachedFrameIndex = this._cachedFrameIndices!!.get(cacheFrameIndex)
            if (cachedFrameIndex >= 0 && this._cachedFrameIndex == cachedFrameIndex) { // Same cache.
                this._transformDirty = false
            } else if (cachedFrameIndex >= 0) { // Has been Cached.
                this._transformDirty = true
                this._cachedFrameIndex = cachedFrameIndex
            } else {
                if (this.constraints.size() > 0) { // Update constraints.
                    for (constraint in this.constraints) {
                        constraint.update()
                    }
                }

                if (this._transformDirty || this.parent != null && this.parent!!._childrenTransformDirty) { // Dirty.
                    this._transformDirty = true
                    this._cachedFrameIndex = -1
                } else if (this._cachedFrameIndex >= 0) { // Same cache, but not set index yet.
                    this._transformDirty = false
                    this._cachedFrameIndices!!.set(cacheFrameIndex, this._cachedFrameIndex)
                } else { // Dirty.
                    this._transformDirty = true
                    this._cachedFrameIndex = -1
                }
            }
        } else {
            if (this.constraints.size() > 0) { // Update constraints.
                for (constraint in this.constraints) {
                    constraint.update()
                }
            }

            if (this._transformDirty || this.parent != null && this.parent!!._childrenTransformDirty) { // Dirty.
                cacheFrameIndex = -1
                this._transformDirty = true
                this._cachedFrameIndex = -1
            }
        }

        if (this._transformDirty) {
            this._transformDirty = false
            this._childrenTransformDirty = true

            if (this._cachedFrameIndex < 0) {
                val isCache = cacheFrameIndex >= 0
                if (this._localDirty) {
                    this._updateGlobalTransformMatrix(isCache)
                }

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
        } else if (this._childrenTransformDirty) {
            this._childrenTransformDirty = false
        }

        this._localDirty = true
    }

    /**
     * @internal
     * @private
     */
    fun updateByConstraint() {
        if (this._localDirty) {
            this._localDirty = false
            if (this._transformDirty || this.parent != null && this.parent!!._childrenTransformDirty) {
                this._updateGlobalTransformMatrix(true)
            }

            this._transformDirty = true
        }
    }

    /**
     * @internal
     * @private
     */
    fun addConstraint(constraint: Constraint) {
        if (this.constraints.indexOf(constraint) < 0) {
            this.constraints.add(constraint)
        }
    }

    /**
     * 下一帧更新变换。 (当骨骼没有动画状态或动画状态播放完成时，骨骼将不在更新)
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    fun invalidUpdate() {
        this._transformDirty = true
    }

    /**
     * 是否包含骨骼或插槽。
     *
     * @returns
     * @version DragonBones 3.0
     * @language zh_CN
     * @see TransformObject
     */
    operator fun contains(child: TransformObject): Boolean {
        if (child === this) {
            return false
        }

        var ancestor: TransformObject? = child
        while (ancestor !== this && ancestor != null) {
            ancestor = ancestor.parent
        }

        return ancestor === this
    }
}
