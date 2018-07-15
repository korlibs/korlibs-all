package com.soywiz.korma

import com.soywiz.kds.*
import com.soywiz.korma.geom.*

inline fun <T> Array2<T>.get(p: IPoint2d): T = get(p.x.toInt(), p.y.toInt())
inline fun <T> Array2<T>.set(p: IPoint2d, value: T) = set(p.x.toInt(), p.y.toInt(), value)

inline fun <T> Array2<T>.tryGet(p: IPoint2d): T? = tryGet(p.x.toInt(), p.y.toInt())
inline fun <T> Array2<T>.trySet(p: IPoint2d, value: T) = trySet(p.x.toInt(), p.y.toInt(), value)
