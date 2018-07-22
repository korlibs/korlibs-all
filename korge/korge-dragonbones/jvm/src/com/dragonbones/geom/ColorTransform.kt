package com.dragonbones.geom

/**
 * @private
 */
class ColorTransform @JvmOverloads constructor(
    var alphaMultiplier: Float = 1f,
    var redMultiplier: Float = 1f,
    var greenMultiplier: Float = 1f,
    var blueMultiplier: Float = 1f,
    var alphaOffset: Int = 0,
    var redOffset: Int = 0,
    var greenOffset: Int = 0,
    var blueOffset: Int = 0
) {

    fun copyFrom(value: ColorTransform) {
        this.alphaMultiplier = value.alphaMultiplier
        this.redMultiplier = value.redMultiplier
        this.greenMultiplier = value.greenMultiplier
        this.blueMultiplier = value.blueMultiplier
        this.alphaOffset = value.alphaOffset
        this.redOffset = value.redOffset
        this.greenOffset = value.greenOffset
        this.blueOffset = value.blueOffset
    }

    fun identity() {
        this.blueMultiplier = 1f
        this.greenMultiplier = this.blueMultiplier
        this.redMultiplier = this.greenMultiplier
        this.alphaMultiplier = this.redMultiplier
        this.blueOffset = 0
        this.greenOffset = this.blueOffset
        this.redOffset = this.greenOffset
        this.alphaOffset = this.redOffset
    }
}/*
    public ColorTransform(
        float alphaMultiplier = 1f, float redMultiplier = 1f, float greenMultiplier = 1f, float blueMultiplier = 1f,
        int alphaOffset = 0, int redOffset = 0, int greenOffset = 0, int blueOffset = 0
    ) {
    }
    */
