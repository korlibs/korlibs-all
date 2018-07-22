package com.dragonbones.armature

import com.dragonbones.core.BaseObject
import com.dragonbones.geom.Matrix
import com.dragonbones.geom.Point
import com.dragonbones.geom.Transform

/**
 * @private
 * @internal
 */
abstract class Constraint : BaseObject() {

    var target: Bone? = null
    var bone: Bone? = null
    var root: Bone? = null

    override fun _onClear() {
        this.target = null //
        this.bone = null //
        this.root = null //
    }

    abstract fun update()

    companion object {
        protected val _helpMatrix = Matrix()
        protected val _helpTransform = Transform()
        protected val _helpPoint = Point()
    }
}
