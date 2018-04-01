package com.soywiz.kmedialayer.scene.geom

import kotlin.math.*

data class Point(var x: Double = 0.0, var y: Double = x) {
    constructor(x: Float, y: Float) : this(x.toDouble(), y.toDouble())
    constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())
    constructor(x: Long, y: Long) : this(x.toDouble(), y.toDouble())
    constructor(v: Point) : this(v.x, v.y)
    //inline constructor(x: Number, y: Number) : this(x.toDouble(), y.toDouble()) // @TODO: Suggest to avoid boxing?

    inline fun setTo(x: Number, y: Number): Point = setTo(x.toDouble(), y.toDouble())

    fun setTo(x: Double, y: Double): Point {
        this.x = x
        this.y = y
        return this
    }

    fun setToZero() = setTo(0.0, 0.0)

    /// Negate this point.
    fun neg() = setTo(-x, -y)

    fun mul(s: Double) = setTo(x * s, y * s)
    fun add(p: Point) = this.setToAdd(this, p)
    fun sub(p: Point) = this.setToSub(this, p)

    fun copyFrom(that: Point) = setTo(that.x, that.y)

    fun setToTransform(mat: Matrix2d, p: Point): Point = setToTransform(mat, p.x, p.y)

    fun setToTransform(mat: Matrix2d, x: Double, y: Double): Point = setTo(
        mat.transformX(x, y),
        mat.transformY(x, y)
    )

    fun setToAdd(a: Point, b: Point): Point = setTo(a.x + b.x, a.y + b.y)
    fun setToSub(a: Point, b: Point): Point = setTo(a.x - b.x, a.y - b.y)
    fun setToMul(a: Point, b: Point): Point = setTo(a.x * b.x, a.y * b.y)
    fun setToMul(a: Point, s: Double): Point = setTo(a.x * s, a.y * s)

    operator fun plusAssign(that: Point) {
        setTo(this.x + that.x, this.y + that.y)
    }

    fun normalize() {
        val len = this.length
        this.setTo(this.x / len, this.y / len)
    }

    val unit: Point get() = this / length
    val length: Double get() = hypot(x, y)
    operator fun plus(that: Point) = Point(this.x + that.x, this.y + that.y)
    operator fun minus(that: Point) = Point(this.x - that.x, this.y - that.y)
    operator fun times(that: Point) = this.x * that.x + this.y * that.y
    operator fun times(v: Double) = Point(x * v, y * v)
    operator fun div(v: Double) = Point(x / v, y / v)

    fun distanceTo(x: Double, y: Double) = hypot(x - this.x, y - this.y)
    fun distanceTo(that: Point) = distanceTo(that.x, that.y)

    override fun toString(): String = "Point($x, $y)"

    companion object {
        fun middle(a: Point, b: Point): Point = Point((a.x + b.x) * 0.5, (a.y + b.y) * 0.5)

        fun angle(a: Point, b: Point): Double = acos((a * b) / (a.length * b.length))

        fun angle(ax: Double, ay: Double, bx: Double, by: Double): Double = acos(((ax * bx) + (ay * by)) / (hypot(ax, ay) * hypot(bx, by)))

        fun sortPoints(points: ArrayList<Point>): Unit {
            points.sortWith(Comparator({ l, r -> cmpPoints(l, r) }))
        }

        protected fun cmpPoints(l: Point, r: Point): Int {
            var ret: Double = l.y - r.y
            if (ret == 0.0) ret = l.x - r.x
            if (ret < 0) return -1
            if (ret > 0) return +1
            return 0
        }

        fun angle(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double): Double {
            val ax = x1 - x2
            val ay = y1 - y2
            val al = hypot(ax, ay)

            val bx = x1 - x3
            val by = y1 - y3
            val bl = hypot(bx, by)

            return acos((ax * bx + ay * by) / (al * bl))
        }
    }
}

// @TODO: Check if this avoid boxing!
inline fun Point(x: Number, y: Number) = Point(x.toDouble(), y.toDouble())

operator fun Point.times(that: Point) = this.x * that.x + this.y * that.y
fun Point.distanceTo(x: Double, y: Double) = hypot(x - this.x, y - this.y)
fun Point.distanceTo(that: Point) = distanceTo(that.x, that.y)