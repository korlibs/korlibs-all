package com.dragonbones.armature

import com.dragonbones.core.BaseObject
import com.dragonbones.geom.Matrix
import com.dragonbones.geom.Point
import com.dragonbones.geom.Transform

/**
 * 基础变换对象。
 *
 * @version DragonBones 4.5
 * @language zh_CN
 */
abstract class TransformObject : BaseObject() {
    /**
     * 对象的名称。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var name: String? = null
    /**
     * 相对于骨架坐标系的矩阵。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    val globalTransformMatrix = Matrix()
    /**
     * 相对于骨架坐标系的变换。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     * @see Transform
     */
    val global = Transform()
    /**
     * 相对于骨架或父骨骼坐标系的偏移变换。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     * @see Transform
     */
    val offset = Transform()
    /**
     * 相对于骨架或父骨骼坐标系的绑定变换。
     *
     * @version DragonBones 3.0
     * @readOnly
     * @language zh_CN
     * @see Transform
     */
    var origin: Transform? = null
    /**
     * 可以用于存储临时数据。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var userData: Any? = null
    /**
     * @private
     */
    protected var _globalDirty: Boolean = false
    /**
     * @private
     */
    /**
     * 所属的骨架。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     * @see Armature
     */
    var armature: Armature? = null
    /**
     * @private
     */
    /**
     * 所属的父骨骼。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     * @see Bone
     */
    var parent: Bone? = null

    /**
     * @private
     */
    override fun _onClear() {
        this.name = ""
        this.globalTransformMatrix.identity()
        this.global.identity()
        this.offset.identity()
        this.origin = null //
        this.userData = null

        this._globalDirty = false
        this.armature = null //
        this.parent = null //
    }

    /**
     * @internal
     * @private
     */
    open fun _setArmature(value: Armature?) {
        this.armature = value
    }

    /**
     * @internal
     * @private
     */
    fun _setParent(value: Bone?) {
        this.parent = value
    }

    /**
     * @private
     */
    fun updateGlobalTransform() {
        if (this._globalDirty) {
            this._globalDirty = false
            this.global.fromMatrix(this.globalTransformMatrix)
        }
    }

    companion object {
        /**
         * @private
         */
        internal val _helpMatrix = Matrix()
        /**
         * @private
         */
		internal val _helpTransform = Transform()
        /**
         * @private
         */
		internal val _helpPoint = Point()
    }
}
