package com.dragonbones.animation

import com.dragonbones.armature.Armature
import com.dragonbones.util.Array

/**
 * WorldClock 提供时钟支持，为每个加入到时钟的 IAnimatable 对象更新时间。
 *
 * @version DragonBones 3.0
 * @language zh_CN
 * @see IAnimatable
 *
 * @see Armature
 */
class WorldClock
/**
 * 创建一个新的 WorldClock 实例。
 * 通常并不需要单独创建 WorldClock 实例，可以直接使用 WorldClock.clock 静态实例。
 * (创建更多独立的 WorldClock 实例可以更灵活的为需要更新的 IAnimateble 实例分组，用于控制不同组不同的播放速度)
 *
 * @version DragonBones 3.0
 * @language zh_CN
 */
@JvmOverloads constructor(time: Float = -1f) : IAnimatable {
    /**
     * 当前时间。 (以秒为单位)
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var time = 0f
    /**
     * 时间流逝速度，用于控制动画变速播放。 [0: 停止播放, (0~1): 慢速播放, 1: 正常播放, (1~N): 快速播放]
     *
     * @default 1f
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var timeScale = 1f
    private val _animatebles = Array<IAnimatable?>()
    private var _clock: WorldClock? = null

    init {
        if (time < 0f) {
            this.time = System.currentTimeMillis() * 0.001f
        } else {
            this.time = time
        }
    }

    /**
     * 为所有的 IAnimatable 实例更新时间。
     *
     * @param passedTime 前进的时间。 (以秒为单位，当设置为 -1 时将自动计算当前帧与上一帧的时间差)
     * @version DragonBones 3.0
     * @language zh_CN
     */
    override fun advanceTime(passedTime: Float) {
        var passedTime = passedTime
        if (passedTime != passedTime) { // isNaN
            passedTime = 0f
        }

        if (passedTime < 0f) {
            passedTime = System.currentTimeMillis() * 0.001f - this.time
        }

        if (this.timeScale != 1f) {
            passedTime *= this.timeScale
        }

        if (passedTime < 0f) {
            this.time -= passedTime
        } else {
            this.time += passedTime
        }

        if (passedTime == 0f) {
            return
        }

        var i = 0
        var r = 0
        var l = this._animatebles.size()
        while (i < l) {
            val animatable = this._animatebles.get(i)
            if (animatable != null) {
                if (r > 0) {
                    this._animatebles.set(i - r, animatable)
                    this._animatebles.set(i, null)
                }

                animatable.advanceTime(passedTime)
            } else {
                r++
            }
            ++i
        }

        if (r > 0) {
            l = this._animatebles.size()
            while (i < l) {
                val animateble = this._animatebles.get(i)
                if (animateble != null) {
                    this._animatebles.set(i - r, animateble)
                } else {
                    r++
                }
                ++i
            }

            this._animatebles.length = this._animatebles.size() - r
        }
    }

    /**
     * 是否包含 IAnimatable 实例
     *
     * @param value IAnimatable 实例。
     * @version DragonBones 3.0
     * @language zh_CN
     */
    operator fun contains(value: IAnimatable): Boolean {
        return this._animatebles.indexOf(value) >= 0
    }

    /**
     * 添加 IAnimatable 实例。
     *
     * @param value IAnimatable 实例。
     * @version DragonBones 3.0
     * @language zh_CN
     */
    fun add(value: IAnimatable) {
        if (this._animatebles.indexOf(value) < 0) {
            this._animatebles.add(value)
            value.clock = this
        }
    }

    /**
     * 移除 IAnimatable 实例。
     *
     * @param value IAnimatable 实例。
     * @version DragonBones 3.0
     * @language zh_CN
     */
    fun remove(value: IAnimatable) {
        val index = this._animatebles.indexOf(value)
        if (index >= 0) {
			this._animatebles[index] = null
            value.clock = null
        }
    }

    /**
     * 清除所有的 IAnimatable 实例。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    fun clear() {
        for (animatable in this._animatebles) {
            if (animatable != null) {
                animatable.clock = null
            }
        }
    }

	override var clock: WorldClock?
		get() {
			return _clock
		}
		set(value) {
			if (this._clock === value) {
				return
			}

			if (this._clock != null) {
				this._clock!!.remove(this)
			}

			this._clock = value

			if (this._clock != null) {
				this._clock!!.add(this)
			}
		}

    companion object {
        /**
         * 一个可以直接使用的全局 WorldClock 实例.
         *
         * @version DragonBones 3.0
         * @language zh_CN
         */
        val clock = WorldClock()
    }
}
/**
 * 创建一个新的 WorldClock 实例。
 * 通常并不需要单独创建 WorldClock 实例，可以直接使用 WorldClock.clock 静态实例。
 * (创建更多独立的 WorldClock 实例可以更灵活的为需要更新的 IAnimateble 实例分组，用于控制不同组不同的播放速度)
 *
 * @version DragonBones 3.0
 * @language zh_CN
 */
