package com.dragonbones.geom

import com.dragonbones.util.FloatArray

/**
 * 2D 矩阵。
 *
 * @version DragonBones 3.0
 * @language zh_CN
 */
class Matrix @JvmOverloads constructor(
    var a: Float = 1f,
    var b: Float = 0f,
    var c: Float = 0f,
    var d: Float = 1f,
    var tx: Float = 0f,
    var ty: Float = 0f
) {

    /**
     * @private
     */
    override fun toString(): String {
        return "[object dragonBones.Matrix] a:" + this.a + " b:" + this.b + " c:" + this.c + " d:" + this.d + " tx:" + this.tx + " ty:" + this.ty
    }

    /**
     * @private
     */
    fun copyFrom(value: Matrix): Matrix {
        this.a = value.a
        this.b = value.b
        this.c = value.c
        this.d = value.d
        this.tx = value.tx
        this.ty = value.ty

        return this
    }

    /**
     * @private
     */
    @JvmOverloads
    fun copyFromArray(value: FloatArray, offset: Int = 0): Matrix {
        this.a = value.get(offset)
        this.b = value.get(offset + 1)
        this.c = value.get(offset + 2)
        this.d = value.get(offset + 3)
        this.tx = value.get(offset + 4)
        this.ty = value.get(offset + 5)

        return this
    }

    /**
     * 转换为单位矩阵。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    fun identity(): Matrix {
        this.d = 1f
        this.a = this.d
        this.c = 0f
        this.b = this.c
        this.ty = 0f
        this.tx = this.ty

        return this
    }

    /**
     * 将当前矩阵与另一个矩阵相乘。
     *
     * @param value 需要相乘的矩阵。
     * @version DragonBones 3.0
     * @language zh_CN
     */
    fun concat(value: Matrix): Matrix {
        var aA = this.a * value.a
        var bA = 0f
        var cA = 0f
        var dA = this.d * value.d
        var txA = this.tx * value.a + value.tx
        var tyA = this.ty * value.d + value.ty

        if (this.b != 0f || this.c != 0f) {
            aA += this.b * value.c
            bA += this.b * value.d
            cA += this.c * value.a
            dA += this.c * value.b
        }

        if (value.b != 0f || value.c != 0f) {
            bA += this.a * value.b
            cA += this.d * value.c
            txA += this.ty * value.c
            tyA += this.tx * value.b
        }

        this.a = aA
        this.b = bA
        this.c = cA
        this.d = dA
        this.tx = txA
        this.ty = tyA

        return this
    }

    /**
     * 转换为逆矩阵。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    fun invert(): Matrix {
        var aA = this.a
        var bA = this.b
        var cA = this.c
        var dA = this.d
        val txA = this.tx
        val tyA = this.ty

        if (bA == 0f && cA == 0f) {
            this.c = 0f
            this.b = this.c
            if (aA == 0f || dA == 0f) {
                this.ty = 0f
                this.tx = this.ty
                this.b = this.tx
                this.a = this.b
            } else {
                this.a = 1f / aA
                aA = this.a
                this.d = 1f / dA
                dA = this.d
                this.tx = -aA * txA
                this.ty = -dA * tyA
            }

            return this
        }

        var determinant = aA * dA - bA * cA
        if (determinant == 0f) {
            this.d = 1f
            this.a = this.d
            this.c = 0f
            this.b = this.c
            this.ty = 0f
            this.tx = this.ty

            return this
        }

        determinant = 1f / determinant
        this.a = dA * determinant
        val k = this.a
        this.b = -bA * determinant
        bA = this.b
        this.c = -cA * determinant
        cA = this.c
        this.d = aA * determinant
        dA = this.d
        this.tx = -(k * txA + cA * tyA)
        this.ty = -(bA * txA + dA * tyA)

        return this
    }

    /**
     * 将矩阵转换应用于指定点。
     *
     * @param x      横坐标。
     * @param y      纵坐标。
     * @param result 应用转换之后的坐标。
     * @params delta 是否忽略 tx，ty 对坐标的转换。
     * @version DragonBones 3.0
     * @language zh_CN
     */
    @JvmOverloads
    fun transformPoint(x: Float, y: Float, result: Point, delta: Boolean = false) {
        result.x = this.a * x + this.c * y
        result.y = this.b * x + this.d * y

        if (!delta) {
            result.x += this.tx
            result.y += this.ty
        }
    }
}
/**
 * @param value
 * @private
 */
/**
 * 将矩阵转换应用于指定点。
 *
 * @param x      横坐标。
 * @param y      纵坐标。
 * @param result 应用转换之后的坐标。
 * @params delta 是否忽略 tx，ty 对坐标的转换。
 * @version DragonBones 3.0
 * @language zh_CN
 */
