package com.dragonbones.model

import com.dragonbones.armature.Slot
import com.dragonbones.core.BaseObject
import com.dragonbones.core.BlendMode
import com.dragonbones.geom.ColorTransform

/**
 * 插槽数据。
 *
 * @version DragonBones 3.0
 * @language zh_CN
 * @see Slot
 */
class SlotData : BaseObject() {

    /**
     * @private
     */
    var blendMode: BlendMode
    /**
     * @private
     */
    var displayIndex: Int = 0
    /**
     * @private
     */
    var zOrder: Float = 0.toFloat()
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
    var color: ColorTransform? = null // Initial value.
    /**
     * @private
     */
    var userData: UserData? = null // Initial value.
    /**
     * 所属的父骨骼数据。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     * @see BoneData
     */
    var parent: BoneData? = null

    /**
     * @private
     */
    override fun _onClear() {
        if (this.userData != null) {
            this.userData!!.returnToPool()
        }

        this.blendMode = BlendMode.Normal
        this.displayIndex = 0
        this.zOrder = 0f
        this.name = ""
        this.color = null //
        this.userData = null
        this.parent = null //
    }

    companion object {
        /**
         * @private
         */
        val DEFAULT_COLOR = ColorTransform()

        /**
         * @private
         */
        fun createColor(): ColorTransform {
            return ColorTransform()
        }
    }
}
