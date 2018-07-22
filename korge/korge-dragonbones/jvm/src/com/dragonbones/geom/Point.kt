package com.dragonbones.geom

class Point @JvmOverloads constructor(var x: Float = 0f, var y: Float = 0f) {

    fun copyFrom(value: Point) {
        this.x = value.x
        this.y = value.y
    }

    fun clear() {
        this.y = 0f
        this.x = this.y
    }
}
