package com.dragonbones.model

import com.dragonbones.core.BoundingBoxType
import com.dragonbones.geom.Point
import com.dragonbones.util.FloatArray

/**
 * 多边形边界框。
 *
 * @version DragonBones 5.1
 * @language zh_CN
 */
class PolygonBoundingBoxData : BoundingBoxData() {

    /**
     * @private
     */
    var count: Int = 0
    /**
     * @private
     */
    var offset: Int = 0 // FloatArray.
    /**
     * @private
     */
    var x: Float = 0.toFloat()
    /**
     * @private
     */
    var y: Float = 0.toFloat()
    /**
     * 多边形顶点。
     *
     * @version DragonBones 5.1
     * @language zh_CN
     */
    var vertices: FloatArray? = null // FloatArray.
    /**
     * @private
     */
    var weight: WeightData? = null // Initial value.

    /**
     * @private
     */
    override fun _onClear() {
        super._onClear()

        if (this.weight != null) {
            this.weight!!.returnToPool()
        }

        this.type = BoundingBoxType.Polygon
        this.count = 0
        this.offset = 0
        this.x = 0f
        this.y = 0f
        this.vertices = null //
        this.weight = null
    }

    /**
     * @inherDoc
     */
    override fun containsPoint(pX: Float, pY: Float): Boolean {
        var isInSide = false
        if (pX >= this.x && pX <= this.width && pY >= this.y && pY <= this.height) {
            var i = 0
            val l = this.count
            var iP = l - 2
            while (i < l) {
                val yA = this.vertices!!.get(this.offset + iP + 1)
                val yB = this.vertices!!.get(this.offset + i + 1)
                if (yB < pY && yA >= pY || yA < pY && yB >= pY) {
                    val xA = this.vertices!!.get(this.offset + iP)
                    val xB = this.vertices!!.get(this.offset + i)
                    if ((pY - yB) * (xA - xB) / (yA - yB) + xB < pX) {
                        isInSide = !isInSide
                    }
                }

                iP = i
                i += 2
            }
        }

        return isInSide
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
        var intersectionCount = 0
        if (RectangleBoundingBoxData.rectangleIntersectsSegment(
                xA,
                yA,
                xB,
                yB,
                this.x,
                this.y,
                this.width,
                this.height,
                null,
                null,
                null
            ) != 0
        ) {
            intersectionCount = PolygonBoundingBoxData.polygonIntersectsSegment(
                xA, yA, xB, yB,
                this.vertices!!, this.offset, this.count,
                intersectionPointA, intersectionPointB, normalRadians
            )
        }

        return intersectionCount
    }

    companion object {

        /**
         * @private
         */
        @JvmOverloads
        fun polygonIntersectsSegment(
            xA: Float, yA: Float, xB: Float, yB: Float,
            vertices: FloatArray, offset: Int, count: Int,
            intersectionPointA: Point? = null,
            intersectionPointB: Point? = null,
            normalRadians: Point? = null
        ): Int {
			var xA = xA
			var xB = xB
			var yA = yA
			var yB = yB

			if (xA == xB) {
				xA = xB + 0.000001f
			}

			if (yA == yB) {
				yA = yB + 0.000001f
			}

			val dXAB = xA - xB
			val dYAB = yA - yB
			val llAB = xA * yB - yA * xB
			var intersectionCount = 0
			var xC = vertices[offset + count - 2]
			var yC = vertices[offset + count - 1]
			var dMin = 0f
			var dMax = 0f
			var xMin = 0f
			var yMin = 0f
			var xMax = 0f
			var yMax = 0f

			var i = 0
			while (i < count) {
				val xD = vertices.get(offset + i)
				val yD = vertices.get(offset + i + 1)

				if (xC == xD) {
					xC = xD + 0.0001f
				}

				if (yC == yD) {
					yC = yD + 0.0001f
				}

				val dXCD = xC - xD
				val dYCD = yC - yD
				val llCD = xC * yD - yC * xD
				val ll = dXAB * dYCD - dYAB * dXCD
				val x = (llAB * dXCD - dXAB * llCD) / ll

				if ((x >= xC && x <= xD || x >= xD && x <= xC) && (dXAB == 0f || x >= xA && x <= xB || x >= xB && x <= xA)) {
					val y = (llAB * dYCD - dYAB * llCD) / ll
					if ((y >= yC && y <= yD || y >= yD && y <= yC) && (dYAB == 0f || y >= yA && y <= yB || y >= yB && y <= yA)) {
						if (intersectionPointB != null) {
							var d = x - xA
							if (d < 0f) {
								d = -d
							}

							if (intersectionCount == 0) {
								dMin = d
								dMax = d
								xMin = x
								yMin = y
								xMax = x
								yMax = y

								if (normalRadians != null) {
									normalRadians.x = (Math.atan2(
										(yD - yC).toDouble(),
										(xD - xC).toDouble()
									) - Math.PI * 0.5f).toFloat()
									normalRadians.y = normalRadians.x
								}
							} else {
								if (d < dMin) {
									dMin = d
									xMin = x
									yMin = y

									if (normalRadians != null) {
										normalRadians.x = (Math.atan2(
											(yD - yC).toDouble(),
											(xD - xC).toDouble()
										) - Math.PI * 0.5f).toFloat()
									}
								}

								if (d > dMax) {
									dMax = d
									xMax = x
									yMax = y

									if (normalRadians != null) {
										normalRadians.y = (Math.atan2(
											(yD - yC).toDouble(),
											(xD - xC).toDouble()
										) - Math.PI * 0.5f).toFloat()
									}
								}
							}

							intersectionCount++
						} else {
							xMin = x
							yMin = y
							xMax = x
							yMax = y
							intersectionCount++

							if (normalRadians != null) {
								normalRadians.x = (Math.atan2(
									(yD - yC).toDouble(),
									(xD - xC).toDouble()
								) - Math.PI * 0.5f).toFloat()
								normalRadians.y = normalRadians.x
							}
							break
						}
					}
				}

				xC = xD
				yC = yD
				i += 2
			}

			if (intersectionCount == 1) {
				if (intersectionPointA != null) {
					intersectionPointA.x = xMin
					intersectionPointA.y = yMin
				}

				if (intersectionPointB != null) {
					intersectionPointB.x = xMin
					intersectionPointB.y = yMin
				}

				if (normalRadians != null) {
					normalRadians.y = (normalRadians.x + Math.PI) as Float
				}
			} else if (intersectionCount > 1) {
				intersectionCount++

				if (intersectionPointA != null) {
					intersectionPointA.x = xMin
					intersectionPointA.y = yMin
				}

				if (intersectionPointB != null) {
					intersectionPointB.x = xMax
					intersectionPointB.y = yMax
				}
			}

			return intersectionCount
        }
    }
}
