package com.dragonbones.model

import com.dragonbones.core.ArmatureType
import com.dragonbones.core.BaseObject
import com.dragonbones.geom.Matrix
import com.dragonbones.geom.Rectangle
import com.dragonbones.geom.Transform
import com.dragonbones.util.Array
import com.dragonbones.util.Console
import com.dragonbones.util.FloatArray

import java.util.HashMap

/**
 * 骨架数据。
 *
 * @version DragonBones 3.0
 * @language zh_CN
 */
class ArmatureData : BaseObject() {
    /**
     * @private
     */
    var type: ArmatureType
    /**
     * 动画帧率。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var frameRate: Float = 0.toFloat()
    /**
     * @private
     */
    var cacheFrameRate: Float = 0.toFloat()
    /**
     * @private
     */
    var scale: Float = 0.toFloat()
    /**
     * 数据名称。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var name: String
    /**
     * @private
     */
    val aabb = Rectangle()
    /**
     * 所有动画数据名称。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    val animationNames = Array<String>()
    /**
     * @private
     */
    val sortedBones = Array<BoneData>()
    /**
     * @private
     */
    val sortedSlots = Array<SlotData>()
    /**
     * @private
     */
    val defaultActions = Array<ActionData>()
    /**
     * @private
     */
    val actions = Array<ActionData>()
    /**
     * 所有骨骼数据。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     * @see BoneData
     */
    val bones: MutableMap<String, BoneData> = HashMap()
    /**
     * 所有插槽数据。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     * @see SlotData
     */
    val slots: MutableMap<String, SlotData> = HashMap()
    /**
     * 所有皮肤数据。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     * @see SkinData
     */
    val skins: MutableMap<String, SkinData> = HashMap()
    /**
     * 所有动画数据。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     * @see AnimationData
     */
    val animations: MutableMap<String, AnimationData> = HashMap()
    /**
     * 获取默认皮肤数据。
     *
     * @version DragonBones 4.5
     * @language zh_CN
     * @see SkinData
     */
    var defaultSkin: SkinData? = null
    /**
     * 获取默认动画数据。
     *
     * @version DragonBones 4.5
     * @language zh_CN
     * @see AnimationData
     */
    var defaultAnimation: AnimationData? = null
    /**
     * @private
     */
    var canvas: CanvasData? = null // Initial value.
    /**
     * @private
     */
    var userData: UserData? = null // Initial value.
    /**
     * 所属的龙骨数据。
     *
     * @version DragonBones 4.5
     * @language zh_CN
     * @see DragonBonesData
     */
    var parent: DragonBonesData? = null

    /**
     * @private
     */
    override fun _onClear() {
        for (action in this.defaultActions) {
            action.returnToPool()
        }

        for (action in this.actions) {
            action.returnToPool()
        }

        for (k in this.bones.keys) {
            this.bones[k].returnToPool()
            this.bones.remove(k)
        }

        for (k in this.slots.keys) {
            this.slots[k].returnToPool()
            this.slots.remove(k)
        }

        for (k in this.skins.keys) {
            this.skins[k].returnToPool()
            this.skins.remove(k)
        }

        for (k in this.animations.keys) {
            this.animations[k].returnToPool()
            this.animations.remove(k)
        }

        if (this.canvas != null) {
            this.canvas!!.returnToPool()
        }

        if (this.userData != null) {
            this.userData!!.returnToPool()
        }

        this.type = ArmatureType.Armature
        this.frameRate = 0f
        this.cacheFrameRate = 0f
        this.scale = 1f
        this.name = ""
        this.aabb.clear()
        this.animationNames.clear()
        this.sortedBones.clear()
        this.sortedSlots.clear()
        this.defaultActions.clear()
        this.actions.clear()
        //this.bones.clear();
        //this.slots.clear();
        //this.skins.clear();
        //this.animations.clear();
        this.defaultSkin = null
        this.defaultAnimation = null
        this.canvas = null
        this.userData = null
        this.parent = null //
    }

    /**
     * @private
     */
    fun sortBones() {
        val total = this.sortedBones.size()
        if (total <= 0) {
            return
        }

        val sortHelper = this.sortedBones.copy()
        var index = 0
        var count = 0
        this.sortedBones.clear()
        while (count < total) {
            val bone = sortHelper.get(index++)
            if (index >= total) {
                index = 0
            }

            if (this.sortedBones.indexOfObject(bone) >= 0) {
                continue
            }

            if (bone.constraints.size() > 0) { // Wait constraint.
                var flag = false
                for (constraint in bone.constraints) {
                    if (this.sortedBones.indexOf(constraint.target) < 0) {
                        flag = true
                    }
                }

                if (flag) {
                    continue
                }
            }

            if (bone.parent != null && this.sortedBones.indexOf(bone.parent) < 0) { // Wait parent.
                continue
            }

            this.sortedBones.add(bone)
            count++
        }
    }

    /**
     * @private
     */
    fun cacheFrames(frameRate: Float) {
        if (this.cacheFrameRate > 0) { // TODO clear cache.
            return
        }

        this.cacheFrameRate = frameRate
        for (k in this.animations.keys) {
            this.animations[k].cacheFrames(this.cacheFrameRate)
        }
    }

    /**
     * @private
     */
    fun setCacheFrame(globalTransformMatrix: Matrix, transform: Transform): Int {
        val dataArray = this.parent!!.cachedFrames
        val arrayOffset = dataArray.size()

        dataArray.length = dataArray.size() + 10
        dataArray.set(arrayOffset + 0, globalTransformMatrix.a)
        dataArray.set(arrayOffset + 1, globalTransformMatrix.b)
        dataArray.set(arrayOffset + 2, globalTransformMatrix.c)
        dataArray.set(arrayOffset + 3, globalTransformMatrix.d)
        dataArray.set(arrayOffset + 4, globalTransformMatrix.tx)
        dataArray.set(arrayOffset + 5, globalTransformMatrix.ty)
        dataArray.set(arrayOffset + 6, transform.rotation)
        dataArray.set(arrayOffset + 7, transform.skew)
        dataArray.set(arrayOffset + 8, transform.scaleX)
        dataArray.set(arrayOffset + 9, transform.scaleY)

        return arrayOffset
    }

    /**
     * @private
     */
    fun getCacheFrame(globalTransformMatrix: Matrix, transform: Transform, arrayOffset: Int) {
        val dataArray = this.parent!!.cachedFrames
        globalTransformMatrix.a = dataArray.get(arrayOffset)
        globalTransformMatrix.b = dataArray.get(arrayOffset + 1)
        globalTransformMatrix.c = dataArray.get(arrayOffset + 2)
        globalTransformMatrix.d = dataArray.get(arrayOffset + 3)
        globalTransformMatrix.tx = dataArray.get(arrayOffset + 4)
        globalTransformMatrix.ty = dataArray.get(arrayOffset + 5)
        transform.rotation = dataArray.get(arrayOffset + 6)
        transform.skew = dataArray.get(arrayOffset + 7)
        transform.scaleX = dataArray.get(arrayOffset + 8)
        transform.scaleY = dataArray.get(arrayOffset + 9)
        transform.x = globalTransformMatrix.tx
        transform.y = globalTransformMatrix.ty
    }

    /**
     * @private
     */
    fun addBone(value: BoneData) {
        if (this.bones.containsKey(value.name)) {
            Console.warn("Replace bone: " + value.name)
            this.bones[value.name].returnToPool()
        }

        this.bones[value.name] = value
        this.sortedBones.add(value)
    }

    /**
     * @private
     */
    fun addSlot(value: SlotData) {
        if (this.slots.containsKey(value.name)) {
            Console.warn("Replace slot: " + value.name)
            this.slots[value.name].returnToPool()
        }

        this.slots[value.name] = value
        this.sortedSlots.add(value)
    }

    /**
     * @private
     */
    fun addSkin(value: SkinData) {
        if (this.skins.containsKey(value.name)) {
            Console.warn("Replace skin: " + value.name)
            this.skins[value.name].returnToPool()
        }

        this.skins[value.name] = value
        if (this.defaultSkin == null) {
            this.defaultSkin = value
        }
    }

    /**
     * @private
     */
    fun addAnimation(value: AnimationData) {
        if (this.animations.containsKey(value.name)) {
            Console.warn("Replace animation: " + value.name)
            this.animations[value.name].returnToPool()
        }

        value.parent = this
        this.animations[value.name] = value
        this.animationNames.add(value.name)
        if (this.defaultAnimation == null) {
            this.defaultAnimation = value
        }
    }

    /**
     * 获取骨骼数据。
     *
     * @param name 数据名称。
     * @version DragonBones 3.0
     * @language zh_CN
     * @see BoneData
     */
    fun getBone(name: String): BoneData? {
        return this.bones[name]
    }

    /**
     * 获取插槽数据。
     *
     * @param name 数据名称。
     * @version DragonBones 3.0
     * @language zh_CN
     * @see SlotData
     */
    fun getSlot(name: String): SlotData? {
        return this.slots[name]
    }

    /**
     * 获取皮肤数据。
     *
     * @param name 数据名称。
     * @version DragonBones 3.0
     * @language zh_CN
     * @see SkinData
     */
    fun getSkin(name: String): SkinData? {
        return this.skins[name]
    }

    /**
     * 获取动画数据。
     *
     * @param name 数据名称。
     * @version DragonBones 3.0
     * @language zh_CN
     * @see AnimationData
     */
    fun getAnimation(name: String): AnimationData? {
        return this.animations[name]
    }
}

