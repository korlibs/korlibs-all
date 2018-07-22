package com.dragonbones.core

/**
 * @private
 */
enum class ActionType private constructor(val v: Int) {
    Play(0),
    Frame(10),
    Sound(11);


    companion object {

        var values = values()
    }
}
