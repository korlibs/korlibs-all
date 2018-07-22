package com.dragonbones.model

import com.dragonbones.core.BaseObject
import com.dragonbones.geom.Transform
import com.dragonbones.util.Array

/**
 * 骨骼数据。
 *
 * @version DragonBones 3.0
 * @language zh_CN
 */
class BoneData : BaseObject() {
    /**
     * @private
     */
    var inheritTranslation: Boolean = false
    /**
     * @private
     */
    var inheritRotation: Boolean = false
    /**
     * @private
     */
    var inheritScale: Boolean = false
    /**
     * @private
     */
    var inheritReflection: Boolean = false
    /**
     * @private
     */
    var length: Float = 0.toFloat()
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
    val transform = Transform()
    /**
     * @private
     */
    val constraints = Array<ConstraintData>()
    /**
     * @private
     */
    var userData: UserData? = null // Initial value.
    /**
     * 所属的父骨骼数据。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var parent: BoneData? = null

    /**
     * @private
     */
    override fun _onClear() {
        for (constraint in this.constraints) {
            constraint.returnToPool()
        }

        if (this.userData != null) {
            this.userData!!.returnToPool()
        }

        this.inheritTranslation = false
        this.inheritRotation = false
        this.inheritScale = false
        this.inheritReflection = false
        this.length = 0f
        this.name = ""
        this.transform.identity()
        this.constraints.clear()
        this.userData = null
        this.parent = null
    }
}
