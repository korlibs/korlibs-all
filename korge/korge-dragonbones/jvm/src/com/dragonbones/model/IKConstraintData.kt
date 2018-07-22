package com.dragonbones.model

/**
 * @private
 */
class IKConstraintData : ConstraintData() {
    var bendPositive: Boolean = false
    var scaleEnabled: Boolean = false
    var weight: Float = 0.toFloat()

    override fun _onClear() {
        super._onClear()

        this.bendPositive = false
        this.scaleEnabled = false
        this.weight = 1f
    }
}
