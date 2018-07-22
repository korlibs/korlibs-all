package com.dragonbones.model

import com.dragonbones.core.BoundingBoxType
import com.dragonbones.geom.Point

/**
 * 椭圆边界框。
 *
 * @version DragonBones 5.1
 * @language zh_CN
 */
class EllipseBoundingBoxData : BoundingBoxData() {

    /**
     * @private
     */
    override fun _onClear() {
        super._onClear()

        this.type = BoundingBoxType.Ellipse
    }

    /**
     * @inherDoc
     */
    override fun containsPoint(pX: Float, pY: Float): Boolean {
        var pY = pY
        val widthH = (this.width * 0.5).toFloat()
        if (pX >= -widthH && pX <= widthH) {
            val heightH = (this.height * 0.5).toFloat()
            if (pY >= -heightH && pY <= heightH) {
                pY *= widthH / heightH
                return Math.sqrt((pX * pX + pY * pY).toDouble()) <= widthH
            }
        }

        return false
    }

    fun intersectsSegment(xA: Float, yA: Float, xB: Float, yB: Float): Int {
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
        return EllipseBoundingBoxData.ellipseIntersectsSegment(
            xA, yA, xB, yB,
            0f, 0f, this.width * 0.5f, this.height * 0.5f,
            intersectionPointA, intersectionPointB, normalRadians
        )
    }

    companion object {

        /**
         * @private
         */
        @JvmOverloads
        fun ellipseIntersectsSegment(
            xA: Float, yA: Float, xB: Float, yB: Float,
            xC: Float, yC: Float, widthH: Float, heightH: Float,
            intersectionPointA: Point? = null,
            intersectionPointB: Point? = null,
            normalRadians: Point? = null
        ): Int {
			var xA = xA
			var xB = xB
			var yA = yA
			var yB = yB
			val d = widthH / heightH
			val dd = d * d

			yA *= d
			yB *= d

			val dX = xB - xA
			val dY = yB - yA
			val lAB = Math.sqrt((dX * dX + dY * dY).toDouble()).toFloat()
			val xD = dX / lAB
			val yD = dY / lAB
			val a = (xC - xA) * xD + (yC - yA) * yD
			val aa = a * a
			val ee = xA * xA + yA * yA
			val rr = widthH * widthH
			val dR = rr - ee + aa
			var intersectionCount = 0

			if (dR >= 0f) {
				val dT = Math.sqrt(dR.toDouble()).toFloat()
				val sA = a - dT
				val sB = a + dT
				val inSideA = if (sA < 0f) -1 else if (sA <= lAB) 0 else 1
				val inSideB = if (sB < 0f) -1 else if (sB <= lAB) 0 else 1
				val sideAB = inSideA * inSideB

				if (sideAB < 0) {
					return -1
				} else if (sideAB == 0) {
					if (inSideA == -1) {
						intersectionCount = 2 // 10
						xB = xA + sB * xD
						yB = (yA + sB * yD) / d

						if (intersectionPointA != null) {
							intersectionPointA.x = xB
							intersectionPointA.y = yB
						}

						if (intersectionPointB != null) {
							intersectionPointB.x = xB
							intersectionPointB.y = yB
						}

						if (normalRadians != null) {
							normalRadians.x = Math.atan2((yB / rr * dd).toDouble(), (xB / rr).toDouble()).toFloat()
							normalRadians.y = (normalRadians.x + Math.PI).toFloat()
						}
					} else if (inSideB == 1) {
						intersectionCount = 1 // 01
						xA = xA + sA * xD
						yA = (yA + sA * yD) / d

						if (intersectionPointA != null) {
							intersectionPointA.x = xA
							intersectionPointA.y = yA
						}

						if (intersectionPointB != null) {
							intersectionPointB.x = xA
							intersectionPointB.y = yA
						}

						if (normalRadians != null) {
							normalRadians.x = Math.atan2((yA / rr * dd).toDouble(), (xA / rr).toDouble()).toFloat()
							normalRadians.y = (normalRadians.x + Math.PI).toFloat()
						}
					} else {
						intersectionCount = 3 // 11

						if (intersectionPointA != null) {
							intersectionPointA.x = xA + sA * xD
							intersectionPointA.y = (yA + sA * yD) / d

							if (normalRadians != null) {
								normalRadians.x = Math.atan2((intersectionPointA.y / rr * dd).toDouble(),
									(intersectionPointA.x / rr).toDouble()
								).toFloat()
							}
						}

						if (intersectionPointB != null) {
							intersectionPointB.x = xA + sB * xD
							intersectionPointB.y = (yA + sB * yD) / d

							if (normalRadians != null) {
								normalRadians.y = Math.atan2((intersectionPointB.y / rr * dd).toDouble(),
									(intersectionPointB.x / rr).toDouble()
								).toFloat()
							}
						}
					}
				}
			}

			return intersectionCount
		}
    }
}
