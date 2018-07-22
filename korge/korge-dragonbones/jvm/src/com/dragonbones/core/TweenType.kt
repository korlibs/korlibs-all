package com.dragonbones.core

/**
 * @private
 */
enum class TweenType private constructor(val v: Int) {
    None(0),
    Line(1),
    Curve(2),
    QuadIn(3),
    QuadOut(4),
    QuadInOut(5);


    companion object {

        var values = values()
    }
}
