package com.dragonbones.core

/**
 * @private
 */
enum class BlendMode private constructor(val v: Int) {
    Normal(0),
    Add(1),
    Alpha(2),
    Darken(3),
    Difference(4),
    Erase(5),
    HardLight(6),
    Invert(7),
    Layer(8),
    Lighten(9),
    Multiply(10),
    Overlay(11),
    Screen(12),
    Subtract(13);


    companion object {

        var values = values()
    }
}
