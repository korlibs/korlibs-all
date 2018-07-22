package com.dragonbones.armature

import com.dragonbones.animation.Animation
import com.dragonbones.animation.IAnimatable
import com.dragonbones.animation.WorldClock
import com.dragonbones.core.ActionType
import com.dragonbones.core.BaseObject
import com.dragonbones.core.DragonBones
import com.dragonbones.event.EventStringType
import com.dragonbones.event.IEventDispatcher
import com.dragonbones.geom.Point
import com.dragonbones.model.*
import com.dragonbones.util.Array
import com.dragonbones.util.Console
import com.dragonbones.util.Function
import com.dragonbones.util.ShortArray

import java.util.Objects
import java.util.function.Consumer

/**
 * 骨架，是骨骼动画系统的核心，由显示容器、骨骼、插槽、动画、事件系统构成。
 *
 * @version DragonBones 3.0
 * @language zh_CN
 * @see ArmatureData
 *
 * @see Bone
 *
 * @see Slot
 *
 * @see Animation
 */
class Armature : BaseObject(), IAnimatable {

    /**
     * 是否继承父骨架的动画状态。
     *
     * @default true
     * @version DragonBones 4.5
     * @language zh_CN
     */
    var inheritAnimation: Boolean = false
    /**
     * @private
     */
    var debugDraw: Boolean = false
    /**
     * 获取骨架数据。
     *
     * @version DragonBones 4.5
     * @readonly
     * @language zh_CN
     * @see ArmatureData
     */
    var armatureData: ArmatureData? = null
    /**
     * 用于存储临时数据。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var userData: Any? = null

    private var _debugDraw: Boolean = false
    private var _lockUpdate: Boolean = false
    private var _bonesDirty: Boolean = false
    private var _slotsDirty: Boolean = false
    private var _zOrderDirty: Boolean = false
    private var _flipX: Boolean = false
    private var _flipY: Boolean = false
    /**
     * @internal
     * @private
     */
    var _cacheFrameIndex: Int = 0
    /**
     * 获取所有骨骼。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     * @see Bone
     */
    val bones = Array<Bone>()
    /**
     * 获取所有插槽。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     * @see Slot
     */
    val slots = Array<Slot>()
    private val _actions = Array<ActionData>()
    /**
     * 获得动画控制器。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     * @see Animation
     */
    var animation: Animation? = null
        private set // Initial value.
    /**
     * @pivate
     */
    var proxy: IArmatureProxy? = null
        private set // Initial value.
    ///**
    // * @deprecated
    // * 已废弃，请参考 @see
    // * @see #_display
    // */
    //@Override
    //public Object getDisplay() {
    //    return this._display;
    //}

    /**
     * 获取显示容器，插槽的显示对象都会以此显示容器为父级，根据渲染平台的不同，类型会不同，通常是 DisplayObjectContainer 类型。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var display: Any? = null
        private set
    /**
     * @private
     */
    var _replaceTextureAtlasData: TextureAtlasData? = null // Initial value.
    private var _replacedTexture: Any? = null
    /**
     * @internal
     * @private
     */
    var _dragonBones: DragonBones? = null
    /**
     * @inheritDoc
     */
    override var clock: WorldClock? = null // Initial value.
    /**
     * @internal
     * @private
     */
    /**
     * 获取父插槽。 (当此骨架是某个骨架的子骨架时，可以通过此属性向上查找从属关系)
     *
     * @version DragonBones 4.5
     * @language zh_CN
     * @see Slot
     */
    var parent: Slot? = null

    var flipX: Boolean
        get() = this._flipX
        set(value) {
            if (this._flipX == value) {
                return
            }

            this._flipX = value
            this.invalidUpdate()
        }

    var flipY: Boolean
        get() = this._flipY
        set(value) {
            if (this._flipY == value) {
                return
            }

            this._flipY = value
            this.invalidUpdate()
        }

    /**
     * 动画缓存帧率，当设置的值大于 0 的时，将会开启动画缓存。
     * 通过将动画数据缓存在内存中来提高运行性能，会有一定的内存开销。
     * 帧率不宜设置的过高，通常跟动画的帧率相当且低于程序运行的帧率。
     * 开启动画缓存后，某些功能将会失效，比如 Bone 和 Slot 的 offset 属性等。
     *
     * @version DragonBones 4.5
     * @language zh_CN
     * @see DragonBonesData.frameRate
     *
     * @see ArmatureData.frameRate
     */
    // Set child armature frameRate.
    var cacheFrameRate: Float
        get() = this.armatureData!!.cacheFrameRate
        set(value) {
            if (this.armatureData!!.cacheFrameRate != value) {
                this.armatureData!!.cacheFrames(value)
                for (slot in this.slots) {
                    val childArmature = slot.childArmature
                    if (childArmature != null) {
                        childArmature.cacheFrameRate = value
                    }
                }
            }
        }

    /**
     * 骨架名称。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     * @see ArmatureData.name
     */
    val name: String
        get() = this.armatureData!!.name

    /**
     * @pivate
     */
    val eventDispatcher: IEventDispatcher?
        get() = this.proxy

    /**
     * @language zh_CN
     * 替换骨架的主贴图，根据渲染引擎的不同，提供不同的贴图数据。
     * @version DragonBones 4.5
     */
    var replacedTexture: Any?
        get() = this._replacedTexture
        set(value) {
            if (this._replacedTexture === value) {
                return
            }

            if (this._replaceTextureAtlasData != null) {
                this._replaceTextureAtlasData!!.returnToPool()
                this._replaceTextureAtlasData = null
            }

            this._replacedTexture = value

            for (slot in this.slots) {
                slot.invalidUpdate()
                slot.update(-1)
            }
        }

    private fun _onSortSlots(a: Slot, b: Slot): Int {
        return if (a._zOrder > b._zOrder) 1 else -1
    }

    /**
     * @private
     */
    override fun _onClear() {
        if (this.clock != null) { // Remove clock first.
            this.clock!!.remove(this)
        }

        for (bone in this.bones) {
            bone.returnToPool()
        }

        for (slot in this.slots) {
            slot.returnToPool()
        }

        for (action in this._actions) {
            action.returnToPool()
        }

        if (this.animation != null) {
            this.animation!!.returnToPool()
        }

        if (this.proxy != null) {
            this.proxy!!.clear()
        }

        if (this._replaceTextureAtlasData != null) {
            this._replaceTextureAtlasData!!.returnToPool()
        }

        this.inheritAnimation = true
        this.debugDraw = false
        this.armatureData = null //
        this.userData = null

        this._debugDraw = false
        this._lockUpdate = false
        this._bonesDirty = false
        this._slotsDirty = false
        this._zOrderDirty = false
        this._flipX = false
        this._flipY = false
        this._cacheFrameIndex = -1
        this.bones.clear()
        this.slots.clear()
        this._actions.clear()
        this.animation = null //
        this.proxy = null //
        this.display = null
        this._replaceTextureAtlasData = null
        this._replacedTexture = null
        this._dragonBones = null //
        this.clock = null
        this.parent = null
    }

    private fun _sortBones() {
        val total = this.bones.size()
        if (total <= 0) {
            return
        }

        val sortHelper = this.bones.copy()
        var index = 0
        var count = 0

        this.bones.clear()
        while (count < total) {
            val bone = sortHelper.get(index++)
            if (index >= total) {
                index = 0
            }

            if (this.bones.indexOf(bone) >= 0) {
                continue
            }

            if (bone.constraints.size() > 0) { // Wait constraint.
                var flag = false
                for (constraint in bone.constraints) {
                    if (this.bones.indexOf(constraint.target) < 0) {
                        flag = true
                        break
                    }
                }

                if (flag) {
                    continue
                }
            }

            if (bone.parent != null && this.bones.indexOf(bone.parent!!) < 0) { // Wait parent.
                continue
            }

            this.bones.add(bone)
            count++
        }
    }

    private fun _sortSlots() {
        this.slots.sort(Comparator { a, b -> _onSortSlots(a, b) })
    }

    /**
     * @internal
     * @private
     */
    fun _sortZOrder(slotIndices: ShortArray?, offset: Int) {
        val slotDatas = this.armatureData!!.sortedSlots
        val isOriginal = slotIndices == null

        if (this._zOrderDirty || !isOriginal) {
            var i = 0
            val l = slotDatas.size()
            while (i < l) {
                val slotIndex = if (isOriginal) i else slotIndices!!.get(offset + i)
                if (slotIndex < 0 || slotIndex >= l) {
                    ++i
                    continue
                }

                val slotData = slotDatas.get(slotIndex)
                val slot = this.getSlot(slotData.name)
                slot?._setZorder(i.toFloat())
                ++i
            }

            this._slotsDirty = true
            this._zOrderDirty = !isOriginal
        }
    }

    /**
     * @internal
     * @private
     */
    fun _addBoneToBoneList(value: Bone) {
        if (this.bones.indexOf(value) < 0) {
            this._bonesDirty = true
            this.bones.add(value)
            this.animation!!._timelineDirty = true
        }
    }

    /**
     * @internal
     * @private
     */
    fun _removeBoneFromBoneList(value: Bone) {
        val index = this.bones.indexOf(value)
        if (index >= 0) {
            this.bones.splice(index, 1)
            this.animation!!._timelineDirty = true
        }
    }

    /**
     * @internal
     * @private
     */
    fun _addSlotToSlotList(value: Slot) {
        if (this.slots.indexOf(value) < 0) {
            this._slotsDirty = true
            this.slots.add(value)
            this.animation!!._timelineDirty = true
        }
    }

    /**
     * @internal
     * @private
     */
    fun _removeSlotFromSlotList(value: Slot) {
        val index = this.slots.indexOf(value)
        if (index >= 0) {
            this.slots.splice(index, 1)
            this.animation!!._timelineDirty = true
        }
    }

    /**
     * @internal
     * @private
     */
    fun _bufferAction(action: ActionData, append: Boolean) {
        if (this._actions.indexOf(action) < 0) {
            if (append) {
                this._actions.add(action)
            } else {
                this._actions.unshiftObject(action)
            }
        }
    }

    /**
     * 释放骨架。 (回收到对象池)
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    fun dispose() {
        if (this.armatureData != null) {
            this._lockUpdate = true
            this._dragonBones!!.bufferObject(this)
        }
    }

    /**
     * @private
     */
    fun init(
        armatureData: ArmatureData,
        proxy: IArmatureProxy, display: Any, dragonBones: DragonBones
    ) {
        if (this.armatureData != null) {
            return
        }

        this.armatureData = armatureData
        this.animation = BaseObject.borrowObject(Animation::class.java)
        this.proxy = proxy
        this.display = display
        this._dragonBones = dragonBones

        this.proxy!!.init(this)
        this.animation!!.init(this)
        this.animation!!.animations = this.armatureData!!.animations
    }

    /**
     * 更新骨架和动画。
     *
     * @param passedTime 两帧之间的时间间隔。 (以秒为单位)
     * @version DragonBones 3.0
     * @language zh_CN
     * @see IAnimatable
     *
     * @see WorldClock
     */
    override fun advanceTime(passedTime: Float) {
        if (this._lockUpdate) {
            return
        }

        if (this.armatureData == null) {
            Console._assert(false, "The armature has been disposed.")
            return
        } else if (this.armatureData!!.parent == null) {
            Console._assert(false, "The armature data has been disposed.")
            return
        }

        val prevCacheFrameIndex = this._cacheFrameIndex

        // Update nimation.
        this.animation!!.advanceTime(passedTime)

        // Sort bones and slots.
        if (this._bonesDirty) {
            this._bonesDirty = false
            this._sortBones()
        }

        if (this._slotsDirty) {
            this._slotsDirty = false
            this._sortSlots()
        }

        // Update bones and slots.
        if (this._cacheFrameIndex < 0 || this._cacheFrameIndex != prevCacheFrameIndex) {
            var i = 0
            var l = this.bones.size()
            while (i < l) {
                this.bones.get(i).update(this._cacheFrameIndex)
                ++i
            }

            i = 0
            l = this.slots.size()
            while (i < l) {
                this.slots.get(i).update(this._cacheFrameIndex)
                ++i
            }
        }

        if (this._actions.size() > 0) {
            this._lockUpdate = true
            for (action in this._actions) {
                if (action.type == ActionType.Play) {
                    this.animation!!.fadeIn(action.name)
                }
            }

            this._actions.clear()
            this._lockUpdate = false
        }

        //
        val drawed = this.debugDraw || DragonBones.debugDraw
        if (drawed || this._debugDraw) {
            this._debugDraw = drawed
            this.proxy!!.debugUpdate(this._debugDraw)
        }
    }

    /**
     * 更新骨骼和插槽。 (当骨骼没有动画状态或动画状态播放完成时，骨骼将不在更新)
     *
     * @param boneName          指定的骨骼名称，如果未设置，将更新所有骨骼。
     * @param updateSlotDisplay 是否更新插槽的显示对象。
     * @version DragonBones 3.0
     * @language zh_CN
     * @see Bone
     *
     * @see Slot
     */
    @JvmOverloads
    fun invalidUpdate(boneName: String? = null, updateSlotDisplay: Boolean = false) {
        if (boneName != null && boneName.length > 0) {
            val bone = this.getBone(boneName)
            if (bone != null) {
                bone.invalidUpdate()

                if (updateSlotDisplay) {
                    for (slot in this.slots) {
                        if (slot.parent === bone) {
                            slot.invalidUpdate()
                        }
                    }
                }
            }
        } else {
            for (bone in this.bones) {
                bone.invalidUpdate()
            }

            if (updateSlotDisplay) {
                for (slot in this.slots) {
                    slot.invalidUpdate()
                }
            }
        }
    }

    /**
     * 判断点是否在所有插槽的自定义包围盒内。
     *
     * @param x 点的水平坐标。（骨架内坐标系）
     * @param y 点的垂直坐标。（骨架内坐标系）
     * @version DragonBones 5.0
     * @language zh_CN
     */
    fun containsPoint(x: Float, y: Float): Slot? {
        for (slot in this.slots) {
            if (slot.containsPoint(x, y)) {
                return slot
            }
        }

        return null
    }

    /**
     * 判断线段是否与骨架的所有插槽的自定义包围盒相交。
     *
     * @param xA                 线段起点的水平坐标。（骨架内坐标系）
     * @param yA                 线段起点的垂直坐标。（骨架内坐标系）
     * @param xB                 线段终点的水平坐标。（骨架内坐标系）
     * @param yB                 线段终点的垂直坐标。（骨架内坐标系）
     * @param intersectionPointA 线段从起点到终点与包围盒相交的第一个交点。（骨架内坐标系）
     * @param intersectionPointB 线段从终点到起点与包围盒相交的第一个交点。（骨架内坐标系）
     * @param normalRadians      碰撞点处包围盒切线的法线弧度。 [x: 第一个碰撞点处切线的法线弧度, y: 第二个碰撞点处切线的法线弧度]
     * @returns 线段从起点到终点相交的第一个自定义包围盒的插槽。
     * @version DragonBones 5.0
     * @language zh_CN
     */
    @JvmOverloads
    fun intersectsSegment(
        xA: Float, yA: Float, xB: Float, yB: Float,
        intersectionPointA: Point? = null,
        intersectionPointB: Point? = null,
        normalRadians: Point? = null
    ): Slot? {
        val isV = xA == xB
        var dMin = 0f
        var dMax = 0f
        var intXA = 0f
        var intYA = 0f
        var intXB = 0f
        var intYB = 0f
        var intAN = 0f
        var intBN = 0f
        var intSlotA: Slot? = null
        var intSlotB: Slot? = null

        for (slot in this.slots) {
            val intersectionCount =
                slot.intersectsSegment(xA, yA, xB, yB, intersectionPointA, intersectionPointB, normalRadians)
            if (intersectionCount > 0) {
                if (intersectionPointA != null || intersectionPointB != null) {
                    if (intersectionPointA != null) {
                        var d = if (isV) intersectionPointA.y - yA else intersectionPointA.x - xA
                        if (d < 0f) {
                            d = -d
                        }

                        if (intSlotA == null || d < dMin) {
                            dMin = d
                            intXA = intersectionPointA.x
                            intYA = intersectionPointA.y
                            intSlotA = slot

                            if (normalRadians != null) {
                                intAN = normalRadians.x
                            }
                        }
                    }

                    if (intersectionPointB != null) {
                        var d = intersectionPointB.x - xA
                        if (d < 0f) {
                            d = -d
                        }

                        if (intSlotB == null || d > dMax) {
                            dMax = d
                            intXB = intersectionPointB.x
                            intYB = intersectionPointB.y
                            intSlotB = slot

                            if (normalRadians != null) {
                                intBN = normalRadians.y
                            }
                        }
                    }
                } else {
                    intSlotA = slot
                    break
                }
            }
        }

        if (intSlotA != null && intersectionPointA != null) {
            intersectionPointA.x = intXA
            intersectionPointA.y = intYA

            if (normalRadians != null) {
                normalRadians.x = intAN
            }
        }

        if (intSlotB != null && intersectionPointB != null) {
            intersectionPointB.x = intXB
            intersectionPointB.y = intYB

            if (normalRadians != null) {
                normalRadians.y = intBN
            }
        }

        return intSlotA
    }

    /**
     * 获取指定名称的骨骼。
     *
     * @param name 骨骼的名称。
     * @returns 骨骼。
     * @version DragonBones 3.0
     * @language zh_CN
     * @see Bone
     */
    fun getBone(name: String): Bone? {
        for (bone in this.bones) {
            if (bone.name == name) {
                return bone
            }
        }

        return null
    }

    /**
     * 通过显示对象获取骨骼。
     *
     * @param display 显示对象。
     * @returns 包含这个显示对象的骨骼。
     * @version DragonBones 3.0
     * @language zh_CN
     * @see Bone
     */
    fun getBoneByDisplay(display: Any): Bone? {
        val slot = this.getSlotByDisplay(display)
        return slot?.parent
    }

    /**
     * 获取插槽。
     *
     * @param name 插槽的名称。
     * @returns 插槽。
     * @version DragonBones 3.0
     * @language zh_CN
     * @see Slot
     */
    fun getSlot(name: String): Slot? {
        for (slot in this.slots) {
            if (slot.name == name) {
                return slot
            }
        }

        return null
    }

    /**
     * 通过显示对象获取插槽。
     *
     * @param display 显示对象。
     * @returns 包含这个显示对象的插槽。
     * @version DragonBones 3.0
     * @language zh_CN
     * @see Slot
     */
    fun getSlotByDisplay(display: Any?): Slot? {
        if (display != null) {
            for (slot in this.slots) {
                if (slot.display === display) {
                    return slot
                }
            }
        }

        return null
    }


    @Deprecated("")
    @JvmOverloads
    fun addBone(value: Bone?, parentName: String? = null) {
        Console._assert(value != null)

        value!!._setArmature(this)
        value._setParent(if (parentName != null) this.getBone(parentName) else null)
    }


    @Deprecated("")
    fun removeBone(value: Bone?) {
        Console._assert(value != null && value.armature === this)

        value!!._setParent(null)
        value._setArmature(null)
    }


    @Deprecated("")
    fun addSlot(value: Slot?, parentName: String) {
        val bone = this.getBone(parentName)

        Console._assert(value != null && bone != null)

        value!!._setArmature(this)
        value._setParent(bone)
    }


    @Deprecated("")
    fun removeSlot(value: Slot?) {
        Console._assert(value != null && value.armature === this)

        value!!._setParent(null)
        value._setArmature(null)
    }

    fun clock(value: WorldClock?) {
        if (this.clock === value) {
            return
        }

        if (this.clock != null) {
            this.clock!!.remove(this)
        }

        this.clock = value

        if (this.clock != null) {
            this.clock!!.add(this)
        }

        // Update childArmature clock.
        for (slot in this.slots) {
            val childArmature = slot.childArmature
            if (childArmature != null) {
                childArmature.clock = this.clock
            }
        }
    }

    /**
     * @see Armature.setReplacedTexture
     */
    @Deprecated("已废弃，请参考 @see")
    fun replaceTexture(texture: Any) {
        this.replacedTexture = texture
    }

    /**
     * @see Armature.getEventDispatcher
     */
    @Deprecated("已废弃，请参考 @see")
    fun hasEventListener(type: EventStringType): Boolean {
        return this.proxy!!.hasEvent(type)
    }

    /**
     * @see Armature.getEventDispatcher
     */
    @Deprecated("已废弃，请参考 @see")
    fun addEventListener(type: EventStringType, listener: Consumer<Any>, target: Any) {
        this.proxy!!.addEvent(type, listener, target)
    }

    /**
     * @see Armature.getEventDispatcher
     */
    @Deprecated("已废弃，请参考 @see")
    fun removeEventListener(type: EventStringType, listener: Consumer<Any>, target: Any) {
        this.proxy!!.removeEvent(type, listener, target)
    }

    /**
     * @see .setCacheFrameRate
     */
    @Deprecated("已废弃，请参考 @see")
    fun enableAnimationCache(frameRate: Float) {
        this.cacheFrameRate = frameRate
    }

}
