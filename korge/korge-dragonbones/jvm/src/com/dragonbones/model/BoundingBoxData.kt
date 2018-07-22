package com.dragonbones.model

import com.dragonbones.core.BaseObject
import com.dragonbones.core.BoundingBoxType
import com.dragonbones.geom.Point

/**
 * 边界框数据基类。
 *
 * @version DragonBones 5.0
 * @language zh_CN
 */
abstract class BoundingBoxData : BaseObject() {
    /**
     * 边界框类型。
     *
     * @version DragonBones 5.0
     * @language zh_CN
     */
    var type: BoundingBoxType? = null
    /**
     * 边界框颜色。
     *
     * @version DragonBones 5.0
     * @language zh_CN
     */
    var color: Int = 0
    /**
     * 边界框宽。（本地坐标系）
     *
     * @version DragonBones 5.0
     * @language zh_CN
     */
    var width: Float = 0.toFloat()
    /**
     * 边界框高。（本地坐标系）
     *
     * @version DragonBones 5.0
     * @language zh_CN
     */
    var height: Float = 0.toFloat()

    /**
     * @private
     */
    override fun _onClear() {
        this.color = 0x000000
        this.width = 0f
        this.height = 0f
    }

    /**
     * 是否包含点。
     *
     * @version DragonBones 5.0
     * @language zh_CN
     */
    abstract fun containsPoint(pX: Float, pY: Float): Boolean

    /**
     * 是否与线段相交。
     *
     * @version DragonBones 5.0
     * @language zh_CN
     */
    abstract fun intersectsSegment(
        xA: Float, yA: Float, xB: Float, yB: Float,
        intersectionPointA: Point?,
        intersectionPointB: Point?,
        normalRadians: Point?
    ): Int
}

