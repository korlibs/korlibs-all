package com.dragonbones.core

/**
 * @version DragonBones 4.5
 * @language zh_CN
 * 动画混合的淡出方式。
 */
enum class AnimationFadeOutMode private constructor(var v: Int) {
    /**
     * 不淡出动画。
     *
     * @version DragonBones 4.5
     * @language zh_CN
     */
    None(0),
    /**
     * 淡出同层的动画。
     *
     * @version DragonBones 4.5
     * @language zh_CN
     */
    SameLayer(1),
    /**
     * 淡出同组的动画。
     *
     * @version DragonBones 4.5
     * @language zh_CN
     */
    SameGroup(2),
    /**
     * 淡出同层并且同组的动画。
     *
     * @version DragonBones 4.5
     * @language zh_CN
     */
    SameLayerAndGroup(3),
    /**
     * 淡出所有动画。
     *
     * @version DragonBones 4.5
     * @language zh_CN
     */
    All(4),
    /**
     * 不替换同名动画。
     *
     * @version DragonBones 5.1
     * @language zh_CN
     */
    Single(5)
}
