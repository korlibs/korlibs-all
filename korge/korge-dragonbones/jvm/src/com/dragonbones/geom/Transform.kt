package com.dragonbones.geom

/**
 * 2D 变换。
 *
 * @version DragonBones 3.0
 * @language zh_CN
 */
class Transform @JvmOverloads constructor(
    /**
     * 水平位移。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var x: Float = 0f,
    /**
     * 垂直位移。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var y: Float = 0f,
    /**
     * 倾斜。 (以弧度为单位)
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var skew: Float = 0f,
    /**
     * 旋转。 (以弧度为单位)
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var rotation: Float = 0f,
    /**
     * 水平缩放。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var scaleX: Float = 1f,
    /**
     * 垂直缩放。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var scaleY: Float = 1f
) {

    /**
     * @private
     */
    override fun toString(): String {
        return "[object dragonBones.Transform] x:" + this.x + " y:" + this.y + " skewX:" + this.skew * 180.0 / Math.PI + " skewY:" + this.rotation * 180.0 / Math.PI + " scaleX:" + this.scaleX + " scaleY:" + this.scaleY
    }

    /**
     * @private
     */
    fun copyFrom(value: Transform): Transform {
        this.x = value.x
        this.y = value.y
        this.skew = value.skew
        this.rotation = value.rotation
        this.scaleX = value.scaleX
        this.scaleY = value.scaleY

        return this
    }

    /**
     * @private
     */
    fun identity(): Transform {
        this.y = 0f
        this.x = this.y
        this.rotation = 0f
        this.skew = this.rotation
        this.scaleY = 1f
        this.scaleX = this.scaleY

        return this
    }

    /**
     * @private
     */
    fun add(value: Transform): Transform {
        this.x += value.x
        this.y += value.y
        this.skew += value.skew
        this.rotation += value.rotation
        this.scaleX *= value.scaleX
        this.scaleY *= value.scaleY

        return this
    }

    /**
     * @private
     */
    operator fun minus(value: Transform): Transform {
        this.x -= value.x
        this.y -= value.y
        this.skew -= value.skew
        this.rotation -= value.rotation
        this.scaleX /= value.scaleX
        this.scaleY /= value.scaleY

        return this
    }

    /**
     * 矩阵转换为变换。
     *
     * @param matrix 矩阵。
     * @version DragonBones 3.0
     * @language zh_CN
     */
    fun fromMatrix(matrix: Matrix): Transform {
        val backupScaleX = this.scaleX
        val backupScaleY = this.scaleY
        val PI_Q = Transform.PI_Q

        this.x = matrix.tx
        this.y = matrix.ty
        this.rotation = Math.atan((matrix.b / matrix.a).toDouble()).toFloat()
        var skewX = Math.atan((-matrix.c / matrix.d).toDouble()).toFloat()

        this.scaleX =
                (if (this.rotation > -PI_Q && this.rotation < PI_Q) matrix.a / Math.cos(this.rotation.toDouble()) else matrix.b / Math.sin(
                    this.rotation.toDouble()
                )).toFloat()
        this.scaleY =
                (if (skewX > -PI_Q && skewX < PI_Q) matrix.d / Math.cos(skewX.toDouble()) else -matrix.c / Math.sin(
                    skewX.toDouble()
                )).toFloat()

        if (backupScaleX >= 0f && this.scaleX < 0f) {
            this.scaleX = -this.scaleX
            this.rotation = (this.rotation - Math.PI).toFloat()
        }

        if (backupScaleY >= 0f && this.scaleY < 0f) {
            this.scaleY = -this.scaleY
            skewX = (skewX - Math.PI).toFloat()
        }

        this.skew = skewX - this.rotation

        return this
    }

    /**
     * 转换为矩阵。
     *
     * @param matrix 矩阵。
     * @version DragonBones 3.0
     * @language zh_CN
     */
    fun toMatrix(matrix: Matrix): Transform {
        if (this.skew != 0f || this.rotation != 0f) {
            matrix.a = Math.cos(this.rotation.toDouble()).toFloat()
            matrix.b = Math.sin(this.rotation.toDouble()).toFloat()

            if (this.skew == 0f) {
                matrix.c = -matrix.b
                matrix.d = matrix.a
            } else {
                matrix.c = (-Math.sin((this.skew + this.rotation).toDouble())).toFloat()
                matrix.d = Math.cos((this.skew + this.rotation).toDouble()).toFloat()
            }

            if (this.scaleX != 1f) {
                matrix.a *= this.scaleX
                matrix.b *= this.scaleX
            }

            if (this.scaleY != 1f) {
                matrix.c *= this.scaleY
                matrix.d *= this.scaleY
            }
        } else {
            matrix.a = this.scaleX
            matrix.b = 0f
            matrix.c = 0f
            matrix.d = this.scaleY
        }

        matrix.tx = this.x
        matrix.ty = this.y

        return this
    }

    companion object {
        /**
         * @private
         */
        val PI_D = (Math.PI * 2.0).toFloat()
        /**
         * @private
         */
        val PI_H = (Math.PI / 2.0).toFloat()
        /**
         * @private
         */
        val PI_Q = (Math.PI / 4.0).toFloat()
        /**
         * @private
         */
        val RAD_DEG = (180.0 / Math.PI).toFloat()
        /**
         * @private
         */
        val DEG_RAD = (Math.PI / 180.0).toFloat()

        /**
         * @private
         */
        fun normalizeRadian(value: Float): Float {
            var value = value
            value = ((value + Math.PI) % (Math.PI * 2.0)).toFloat()
            value += (if (value > 0f) -Math.PI else Math.PI).toFloat()

            return value
        }
    }
}
