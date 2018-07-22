package com.dragonbones.armature

/**
 * @private
 * @internal
 */
class IKConstraint : Constraint() {
	var bendPositive: Boolean = false
	var scaleEnabled: Boolean = false // TODO
	var weight: Float = 0.toFloat()

	override fun _onClear() {
		super._onClear()

		this.bendPositive = false
		this.scaleEnabled = false
		this.weight = 1f
	}

	private fun _computeA() {
		val ikGlobal = this.target!!.global
		val global = this.bone!!.global
		val globalTransformMatrix = this.bone!!.globalTransformMatrix
		// const boneLength = this.bone.boneData.length;
		// const x = globalTransformMatrix.a * boneLength;

		var ikRadian = Math.atan2((ikGlobal.y - global.y).toDouble(), (ikGlobal.x - global.x).toDouble()).toFloat()
		if (global.scaleX < 0f) {
			ikRadian += Math.PI.toFloat()
		}

		global.rotation += (ikRadian - global.rotation) * this.weight
		global.toMatrix(globalTransformMatrix)
	}

	private fun _computeB() {
		val bone = this.bone!!
		val boneLength = bone.boneData!!.length
		val parent = this.root
		val ikGlobal = this.target!!.global
		val parentGlobal = parent!!.global
		val global = bone.global
		val globalTransformMatrix = bone.globalTransformMatrix

		val x = globalTransformMatrix.a * boneLength
		val y = globalTransformMatrix.b * boneLength

		val lLL = x * x + y * y
		val lL = Math.sqrt(lLL.toDouble()).toFloat()

		var dX = global.x - parentGlobal.x
		var dY = global.y - parentGlobal.y
		val lPP = dX * dX + dY * dY
		val lP = Math.sqrt(lPP.toDouble()).toFloat()
		val rawRadianA = Math.atan2(dY.toDouble(), dX.toDouble()).toFloat()

		dX = ikGlobal.x - parentGlobal.x
		dY = ikGlobal.y - parentGlobal.y
		val lTT = dX * dX + dY * dY
		val lT = Math.sqrt(lTT.toDouble()).toFloat()

		var ikRadianA = 0f
		if (lL + lP <= lT || lT + lL <= lP || lT + lP <= lL) {
			ikRadianA = Math.atan2((ikGlobal.y - parentGlobal.y).toDouble(), (ikGlobal.x - parentGlobal.x).toDouble())
				.toFloat()
			if (lL + lP <= lT) {
			} else if (lP < lL) {
				ikRadianA += Math.PI.toFloat()
			}
		} else {
			val h = ((lPP - lLL + lTT) / (2.0 * lTT)).toFloat()
			val r = (Math.sqrt((lPP - h * h * lTT).toDouble()) / lT).toFloat()
			val hX = parentGlobal.x + (dX * h)
			val hY = parentGlobal.y + (dY * h)
			val rX = -dY * r
			val rY = dX * r

			var isPPR = false
			if (parent.parent != null) {
				val parentParentMatrix = parent.parent!!.globalTransformMatrix
				isPPR = parentParentMatrix.a * parentParentMatrix.d - parentParentMatrix.b * parentParentMatrix.c < 0f
			}

			if (isPPR != this.bendPositive) {
				global.x = hX - rX
				global.y = hY - rY
			} else {
				global.x = hX + rX
				global.y = hY + rY
			}

			ikRadianA =
					Math.atan2((global.y - parentGlobal.y).toDouble(), (global.x - parentGlobal.x).toDouble()).toFloat()
		}

		var dR = (ikRadianA - rawRadianA) * this.weight
		parentGlobal.rotation += dR
		parentGlobal.toMatrix(parent.globalTransformMatrix)

		val parentRadian = rawRadianA + dR
		global.x = (parentGlobal.x + Math.cos(parentRadian.toDouble()) * lP).toFloat()
		global.y = (parentGlobal.y + Math.sin(parentRadian.toDouble()) * lP).toFloat()

		var ikRadianB = Math.atan2((ikGlobal.y - global.y).toDouble(), (ikGlobal.x - global.x).toDouble()).toFloat()
		if (global.scaleX < 0f) {
			ikRadianB += Math.PI.toFloat()
		}

		dR = (ikRadianB - global.rotation) * this.weight
		global.rotation += dR
		global.toMatrix(globalTransformMatrix)
	}

	override fun update() {
		if (this.root == null) {
			this.bone!!.updateByConstraint()
			this._computeA()
		} else {
			this.root!!.updateByConstraint()
			this.bone!!.updateByConstraint()
			this._computeB()
		}
	}
}
