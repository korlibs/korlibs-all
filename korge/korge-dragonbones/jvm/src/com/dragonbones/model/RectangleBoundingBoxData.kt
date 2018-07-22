package com.dragonbones.model

import com.dragonbones.core.BoundingBoxType
import com.dragonbones.geom.Point
import com.dragonbones.model.RectangleBoundingBoxData.*

/**
 * 矩形边界框。
 *
 * @version DragonBones 5.1
 * @language zh_CN
 */
class RectangleBoundingBoxData : BoundingBoxData() {

    /**
     * @private
     */
    override fun _onClear() {
        super._onClear()

        this.type = BoundingBoxType.Rectangle
    }

    /**
     * @inherDoc
     */
    override fun containsPoint(pX: Float, pY: Float): Boolean {
        val widthH = (this.width * 0.5).toFloat()
        if (pX >= -widthH && pX <= widthH) {
            val heightH = (this.height * 0.5).toFloat()
            if (pY >= -heightH && pY <= heightH) {
                return true
            }
        }

        return false
    }

    fun intersectsSegment(
        xA: Float, yA: Float, xB: Float, yB: Float
    ): Int {
        return intersectsSegment(xA, yA, xB, yB, null, null, null)
    }

    /**
     * @inherDoc
     */
    override fun intersectsSegment(
        xA: Float, yA: Float, xB: Float, yB: Float,
        intersectionPointA: Point?,
        intersectionPointB: Point?,
        normalRadians: Point?
    ): Int {
        val widthH = (this.width * 0.5).toFloat()
        val heightH = (this.height * 0.5).toFloat()

        return rectangleIntersectsSegment(
            xA, yA, xB, yB,
            -widthH, -heightH, widthH, heightH,
            intersectionPointA, intersectionPointB, normalRadians
        )
    }

    companion object {
        /**
         * Compute the bit code for a point (x, y) using the clip rectangle
         */
        private fun _computeOutCode(x: Float, y: Float, xMin: Float, yMin: Float, xMax: Float, yMax: Float): Int {
            var code = OutCode.InSide.v  // initialised as being inside of [[clip window]]

            if (x < xMin) {             // to the left of clip window
                code = code or OutCode.Left.v
            } else if (x > xMax) {        // to the right of clip window
                code = code or OutCode.Right.v
            }

            if (y < yMin) {             // below the clip window
                code = code or OutCode.Top.v
            } else if (y > yMax) {        // above the clip window
                code = code or OutCode.Bottom.v
            }

            return code
        }

        /**
         * @private
         */
        @JvmOverloads
        fun rectangleIntersectsSegment(
            xA: Float, yA: Float, xB: Float, yB: Float,
            xMin: Float, yMin: Float, xMax: Float, yMax: Float,
            intersectionPointA: Point? = null,
            intersectionPointB: Point? = null,
            normalRadians: Point? = null
        ): Int {
			var xA = xA
			var xB = xB
			var yA = yA
			var yB = yB

			val inSideA = xA > xMin && xA < xMax && yA > yMin && yA < yMax
			val inSideB = xB > xMin && xB < xMax && yB > yMin && yB < yMax

			if (inSideA && inSideB) {
				return -1
			}

			var intersectionCount = 0
			var outcode0 = RectangleBoundingBoxData._computeOutCode(xA, yA, xMin, yMin, xMax, yMax)
			var outcode1 = RectangleBoundingBoxData._computeOutCode(xB, yB, xMin, yMin, xMax, yMax)

			while (true) {
				if (outcode0 or outcode1 == 0) { // Bitwise OR is 0. Trivially accept and get out of loop
					intersectionCount = 2
					break
				} else if (outcode0 and outcode1 != 0) { // Bitwise AND is not 0. Trivially reject and get out of loop
					break
				}

				// failed both tests, so calculate the line segment to clip
				// from an outside point to an intersection with clip edge
				var x = 0f
				var y = 0f
				var normalRadian = 0f

				// At least one endpoint is outside the clip rectangle; pick it.
				val outcodeOut = if (outcode0 != 0) outcode0 else outcode1

				// Now find the intersection point;
				if (outcodeOut and OutCode.Top.v != 0) {             // point is above the clip rectangle
					x = xA + (xB - xA) * (yMin - yA) / (yB - yA)
					y = yMin

					if (normalRadians != null) {
						normalRadian = (-Math.PI * 0.5).toFloat()
					}
				} else if (outcodeOut and OutCode.Bottom.v != 0) {     // point is below the clip rectangle
					x = xA + (xB - xA) * (yMax - yA) / (yB - yA)
					y = yMax

					if (normalRadians != null) {
						normalRadian = (Math.PI * 0.5).toFloat()
					}
				} else if (outcodeOut and OutCode.Right.v != 0) {      // point is to the right of clip rectangle
					y = yA + (yB - yA) * (xMax - xA) / (xB - xA)
					x = xMax

					if (normalRadians != null) {
						normalRadian = 0f
					}
				} else if (outcodeOut and OutCode.Left.v != 0) {       // point is to the left of clip rectangle
					y = yA + (yB - yA) * (xMin - xA) / (xB - xA)
					x = xMin

					if (normalRadians != null) {
						normalRadian = Math.PI.toFloat()
					}
				}

				// Now we move outside point to intersection point to clip
				// and get ready for next pass.
				if (outcodeOut == outcode0) {
					xA = x
					yA = y
					outcode0 = RectangleBoundingBoxData._computeOutCode(xA, yA, xMin, yMin, xMax, yMax)

					if (normalRadians != null) {
						normalRadians.x = normalRadian
					}
				} else {
					xB = x
					yB = y
					outcode1 = RectangleBoundingBoxData._computeOutCode(xB, yB, xMin, yMin, xMax, yMax)

					if (normalRadians != null) {
						normalRadians.y = normalRadian
					}
				}
			}

			if (intersectionCount != 0) {
				if (inSideA) {
					intersectionCount = 2 // 10

					if (intersectionPointA != null) {
						intersectionPointA.x = xB
						intersectionPointA.y = yB
					}

					if (intersectionPointB != null) {
						intersectionPointB.x = xB
						intersectionPointB.y = xB
					}

					if (normalRadians != null) {
						normalRadians.x = (normalRadians.y + Math.PI) as Float
					}
				} else if (inSideB) {
					intersectionCount = 1 // 01

					if (intersectionPointA != null) {
						intersectionPointA.x = xA
						intersectionPointA.y = yA
					}

					if (intersectionPointB != null) {
						intersectionPointB.x = xA
						intersectionPointB.y = yA
					}

					if (normalRadians != null) {
						normalRadians.y = (normalRadians.x + Math.PI) as Float
					}
				} else {
					intersectionCount = 3 // 11
					if (intersectionPointA != null) {
						intersectionPointA.x = xA
						intersectionPointA.y = yA
					}

					if (intersectionPointB != null) {
						intersectionPointB.x = xB
						intersectionPointB.y = yB
					}
				}
			}

			return intersectionCount
        }
    }
}
