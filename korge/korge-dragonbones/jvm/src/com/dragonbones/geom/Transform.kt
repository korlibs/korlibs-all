package com.dragonbones.geom
/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2012-2018 DragonBones team and other contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

/**
 * - 2D Transform.
 * @version DragonBones 3.0
 * @language en_US
 */
/**
 * - 2D 变换。
 * @version DragonBones 3.0
 * @language zh_CN
 */
class Transform : XY {
	companion object {
		/**
		 * @private
		 */
		val PI: Double = Math.PI
		/**
		 * @private
		 */
		val PI_D: Double = Math.PI * 2.0
		/**
		 * @private
		 */
		val PI_H: Double = Math.PI / 2.0
		/**
		 * @private
		 */
		val PI_Q: Double = Math.PI / 4.0
		/**
		 * @private
		 */
		val RAD_DEG: Double = 180.0 / Math.PI
		/**
		 * @private
		 */
		val DEG_RAD: Double = Math.PI / 180.0

		/**
		 * @private
		 */
		fun normalizeRadian(value: Double): Double {
			var value = (value + Math.PI) % (Math.PI * 2.0)
			value += if (value > 0.0) -Math.PI else Math.PI

			return value
		}
	}

	/**
	 * - Horizontal translate.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 水平位移。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	override var x: Double
	/**
	 * - Vertical translate.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 垂直位移。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	override var y: Double
	/**
	 * - Skew. (In radians)
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 倾斜。 （以弧度为单位）
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var skew: Double
	/**
	 * - rotation. (In radians)
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 旋转。 （以弧度为单位）
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var rotation: Double
	/**
	 * - Horizontal Scaling.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 水平缩放。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var scaleX: Double
	/**
	 * - Vertical scaling.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 垂直缩放。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	var scaleY: Double
	/**
	 * @private
	 */
	constructor(x: Double = 0.0, y: Double = 0.0, skew: Double = 0.0, rotation: Double = 0.0, scaleX: Double = 1.0, scaleY: Double = 1.0) {
		this.x = x
		this.y = y
		this.skew = skew
		this.rotation = rotation
		this.scaleX = scaleX
		this.scaleY = scaleY
	}

	override fun toString(): String {
		return "[object dragonBones.Transform] x:" + this.x + " y:" + this.y + " skewX:" + this.skew * 180.0 / Math.PI + " skewY:" + this.rotation * 180.0 / Math.PI + " scaleX:" + this.scaleX + " scaleY:" + this.scaleY
	}

	/**
	 * @private
	 */
	fun copyFrom(value: Transform): Transform {
		this.x = value.x
		this.y = value.y
		this.skew = value.skew
		this.rotation = value.rotation
		this.scaleX = value.scaleX
		this.scaleY = value.scaleY

		return this
	}

	/**
	 * @private
	 */
	fun identity(): Transform {
		this.x = 0.0
		this.y = 0.0
		this.skew = 0.0
		this.rotation = 0.0
		this.scaleX = 0.0
		this.scaleY = 1.0

		return this
	}

	/**
	 * @private
	 */
	fun add(value: Transform): Transform {
		this.x += value.x
		this.y += value.y
		this.skew += value.skew
		this.rotation += value.rotation
		this.scaleX *= value.scaleX
		this.scaleY *= value.scaleY

		return this
	}

	/**
	 * @private
	 */
	fun minus(value: Transform): Transform {
		this.x -= value.x
		this.y -= value.y
		this.skew -= value.skew
		this.rotation -= value.rotation
		this.scaleX /= value.scaleX
		this.scaleY /= value.scaleY

		return this
	}

	/**
	 * @private
	 */
	fun fromMatrix(matrix: Matrix): Transform {
		val backupScaleX = this.scaleX
		val backupScaleY = this.scaleY
		val PI_Q = Transform.PI_Q

		this.x = matrix.tx
		this.y = matrix.ty
		this.rotation = Math.atan(matrix.b / matrix.a)
		var skewX = Math.atan(-matrix.c / matrix.d)

		this.scaleX = if (this.rotation > -PI_Q && this.rotation < PI_Q) matrix.a / Math.cos(this.rotation) else matrix.b / Math.sin(this.rotation)
		this.scaleY = if (skewX > -PI_Q && skewX < PI_Q) matrix.d / Math.cos(skewX) else -matrix.c / Math.sin(skewX)

		if (backupScaleX >= 0.0 && this.scaleX < 0.0) {
			this.scaleX = -this.scaleX
			this.rotation = this.rotation - Math.PI
		}

		if (backupScaleY >= 0.0 && this.scaleY < 0.0) {
			this.scaleY = -this.scaleY
			skewX = skewX - Math.PI
		}

		this.skew = skewX - this.rotation

		return this
	}

	/**
	 * @private
	 */
	fun toMatrix(matrix: Matrix): Transform {
		if (this.rotation == 0.0) {
			matrix.a = 1.0
			matrix.b = 0.0
		}
		else {
			matrix.a = Math.cos(this.rotation)
			matrix.b = Math.sin(this.rotation)
		}

		if (this.skew == 0.0) {
			matrix.c = -matrix.b
			matrix.d = matrix.a
		}
		else {
			matrix.c = -Math.sin(this.skew + this.rotation)
			matrix.d = Math.cos(this.skew + this.rotation)
		}

		if (this.scaleX != 1.0) {
			matrix.a *= this.scaleX
			matrix.b *= this.scaleX
		}

		if (this.scaleY != 1.0) {
			matrix.c *= this.scaleY
			matrix.d *= this.scaleY
		}

		matrix.tx = this.x
		matrix.ty = this.y

		return this
	}
}
