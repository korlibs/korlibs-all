package com.dragonbones.geom

class Rectangle @JvmOverloads constructor(
    var x: Float = 0f,
    var y: Float = 0f,
    var width: Float = 0f,
    var height: Float = 0f
) {

    fun copyFrom(value: Rectangle) {
        this.x = value.x
        this.y = value.y
        this.width = value.width
        this.height = value.height
    }

    fun clear() {
        this.y = 0f
        this.x = this.y
        this.height = 0f
        this.width = this.height
    }
}
