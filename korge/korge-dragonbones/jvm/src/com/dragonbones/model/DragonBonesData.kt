package com.dragonbones.model

import com.dragonbones.core.BaseObject
import com.dragonbones.util.*
import com.dragonbones.util.FloatArray
import com.dragonbones.util.ShortArray

import java.util.HashMap

/**
 * 龙骨数据。
 * 一个龙骨数据包含多个骨架数据。
 *
 * @version DragonBones 3.0
 * @language zh_CN
 * @see ArmatureData
 */
class DragonBonesData : BaseObject() {
    /**
     * 是否开启共享搜索。
     *
     * @default false
     * @version DragonBones 4.5
     * @language zh_CN
     */
    var autoSearch: Boolean = false
    /**
     * 动画帧频。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var frameRate: Float = 0.toFloat()
    /**
     * 数据版本。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var version: String
    /**
     * 数据名称。(该名称与龙骨项目名保持一致)
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var name: String
    /**
     * @private
     */
    val frameIndices = IntArray()
    /**
     * @private
     */
    val cachedFrames = FloatArray()
    /**
     * 所有骨架数据名称。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     * @see .armatures
     */
    val armatureNames = Array<String>()
    /**
     * 所有骨架数据。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     * @see ArmatureData
     */
    val armatures: MutableMap<String, ArmatureData> = HashMap()
    /**
     * @private
     */
    var intArray: com.dragonbones.util.ShortArray? = null
    /**
     * @private
     */
    var floatArray: com.dragonbones.util.FloatArray? = null
    /**
     * @private
     */
    var frameIntArray: com.dragonbones.util.ShortArray? = null
    /**
     * @private
     */
    var frameFloatArray: com.dragonbones.util.FloatArray? = null
    /**
     * @private
     */
    var frameArray: com.dragonbones.util.ShortArray? = null
    /**
     * @private
     */
    var timelineArray: com.dragonbones.util.CharArray? = null
    /**
     * @private
     */
    var userData: UserData? = null // Initial value.

    /**
     * @private
     */
    override fun _onClear() {
        for (k in this.armatures.keys) {
            this.armatures[k].returnToPool()
            this.armatures.remove(k)
        }

        if (this.userData != null) {
            this.userData!!.returnToPool()
        }

        this.autoSearch = false
        this.frameRate = 0f
        this.version = ""
        this.name = ""
        this.frameIndices.clear()
        this.cachedFrames.clear()
        this.armatureNames.clear()
        //this.armatures.clear();
        this.intArray = null //
        this.floatArray = null //
        this.frameIntArray = null //
        this.frameFloatArray = null //
        this.frameArray = null //
        this.timelineArray = null //
        this.userData = null
    }

    /**
     * @private
     */
    fun addArmature(value: ArmatureData) {
        if (this.armatures.containsKey(value.name)) {
            Console.warn("Replace armature: " + value.name)
            this.armatures[value.name].returnToPool()
        }

        value.parent = this
        this.armatures[value.name] = value
        this.armatureNames.add(value.name)
    }

    /**
     * 获取骨架数据。
     *
     * @param name 骨架数据名称。
     * @version DragonBones 3.0
     * @language zh_CN
     * @see ArmatureData
     */
    fun getArmature(name: String): ArmatureData? {
        return this.armatures[name]
    }


    @Deprecated("已废弃，请参考 @see")
    fun dispose() {
        Console.warn("已废弃，请参考 @see")
        this.returnToPool()
    }
}
