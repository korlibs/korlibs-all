package com.soywiz.kmedialayer.scene.geom

interface Interpolator<T> {
    fun interpolate(ratio: Double, src: T, dst: T): Double

    companion object {
        class InterpolatorAcceptor<T>(val accept: (Any) -> Boolean, val interpolator: Interpolator<T>)

        val interpolators = arrayListOf<InterpolatorAcceptor<*>>()

        inline fun <reified T> register(interpolator: Interpolator<T>) {
            interpolators += InterpolatorAcceptor({ it is T }, interpolator)
        }
    }
}

fun interpolate(ratio: Double, src: Int, dst: Int): Int = (src + (dst - src) * ratio).toInt()
fun interpolate(ratio: Double, src: Float, dst: Float): Float = (src + (dst - src) * ratio).toFloat()
fun interpolate(ratio: Double, src: Double, dst: Double): Double = src + (dst - src) * ratio

fun <T> interpolate(ratio: Double, src: T, dst: T): T {
    // Standard integral interpolators
    when (src) {
        is Double -> return interpolate(ratio, src, (dst as Double)) as T
        is Float -> return interpolate(ratio, src, (dst as Float)) as T
        is Int -> return interpolate(ratio, src, (dst as Int)) as T
    }
    for (interpolator in Interpolator.interpolators) {
        if (interpolator.accept(src as Any)) {
            return (interpolator.interpolator as Interpolator<Any>).interpolate(ratio, src, dst as Any) as T
        }
    }
    throw IllegalArgumentException("Don't know how to interpolate $src with $dst")
}