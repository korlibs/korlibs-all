package com.dragonbones.animation

/**
 * 播放动画接口。 (Armature 和 WordClock 都实现了该接口)
 * 任何实现了此接口的实例都可以加到 WorldClock 实例中，由 WorldClock 统一更新时间。
 *
 * @version DragonBones 3.0
 * @language zh_CN
 * @see WorldClock
 *
 * @see com.dragonbones.armature.Armature
 */
interface IAnimatable {

    /**
     * 当前所属的 WordClock 实例。
     *
     * @version DragonBones 5.0
     * @language zh_CN
     */
    var clock: WorldClock?

    /**
     * 更新时间。
     *
     * @param passedTime 前进的时间。 (以秒为单位)
     * @version DragonBones 3.0
     * @language zh_CN
     */
    fun advanceTime(passedTime: Float)
}
