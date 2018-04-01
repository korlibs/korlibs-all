package com.soywiz.kmedialayer.scene

import com.soywiz.kmedialayer.scene.font.*
import com.soywiz.kmedialayer.scene.geom.*

open class View {
	companion object {
		private val IDENTITY = Matrix2d()
	}

	var parent: ViewContainer? = null
	val root: ViewContainer? get() = parent?.root ?: this as? ViewContainer?
	var validParents = false
	var validChildren = false
	var name: String? = null

	protected val _transform = ViewTransform()
	protected val _globalMatrix = Matrix2d()
	protected var _invGlobalMatrixValid = false
	protected val _invGlobalMatrix = Matrix2d()
	private var _components: ArrayList<Component>? = null
	val components: List<Component>? get() = _components

	inline fun <reified T : Component> getOrCreateComponent(component: (View) -> T): T {
		return (components?.firstOrNull { it is T } as? T) ?: component(this).apply { addComponent(this) }
	}

	fun addComponent(component: Component) {
		if (_components == null) _components = arrayListOf()
		_components?.add(component)
	}

	fun removeComponent(component: Component) {
		val removed = _components?.remove(component) ?: false
		if (removed) {
			//component.dettached()
		}
		if (_components?.size == 0) {
			_components = null
		}
	}

	protected open fun recompute() {
		if (validParents && validChildren) return
		validParents = true
		validChildren = true
		_transform.update()
		val pgm = parent?.globalMatrix ?: IDENTITY
		_globalMatrix.setToIdentity()
		_globalMatrix.premultiply(pgm)
		_globalMatrix.premultiply(_transform.matrix)
		_invGlobalMatrixValid = false
	}

	val localMatrix: Matrix2d
		get() {
			recompute()
			return _transform.matrix
		}

	val localTransform: Matrix2d.Transform
		get() {
			recompute()
			return _transform.transform
		}

	val globalMatrix: Matrix2d
		get() {
			recompute()
			return _globalMatrix
		}

	val invGlobalMatrix: Matrix2d
		get() {
			recompute()
			if (!_invGlobalMatrixValid) {
				_invGlobalMatrix.setToInverse(_globalMatrix)
			}
			return _invGlobalMatrix
		}

	val concatSpeed: Double get() = (parent?.concatSpeed ?: 1.0) * speed
	val concatAlpha: Double get() = (parent?.concatAlpha ?: 1.0) * alpha

	fun globalToLocalX(x: Double, y: Double): Double = invGlobalMatrix.transformX(x, y)
	fun globalToLocalY(x: Double, y: Double): Double = invGlobalMatrix.transformY(x, y)

	fun localToGlobalX(x: Double, y: Double): Double = globalMatrix.transformX(x, y)
	fun localToGlobalY(x: Double, y: Double): Double = globalMatrix.transformY(x, y)

	fun globalToLocal(p: Point, out: Point = Point()): Point = out.setToTransform(invGlobalMatrix, p)
	fun localToGlobal(p: Point, out: Point = Point()): Point = out.setToTransform(globalMatrix, p)

	open fun viewInGlobal(x: Double, y: Double): View? {
		return null
	}

	private val t get() = _transform.transform
	var x; get() = t.x; set(value) = run { invalidate(); t.x = value }
	var y; get() = t.y; set(value) = run { invalidate(); t.y = value }
	var scaleX; get() = t.scaleX; set(value) = run { invalidate(); t.scaleX = value }
	var scaleY; get() = t.scaleY; set(value) = run { invalidate(); t.scaleY = value }
	var rotation; get() = t.rotation; set(value) = run { invalidate(); t.rotation = value }
	var rotationDegrees; get() = t.rotationDegrees; set(value) = run { invalidate(); t.rotationDegrees = value % 360 }
	var alpha = 1.0; set(value) = run { invalidate(); field = value }
	var speed = 1.0

	var scale get() = (scaleX + scaleY) / 2.0; set(value) = run { scaleX = value; scaleY = value }

	inline fun position(x: Number, y: Number) = run { this.x = x.toDouble(); this.y = y.toDouble() }

	fun invalidate() {
		invalidateParent()
		invalidateChildren()
	}

	open fun invalidateChildren() {
		validChildren = false
	}

	private fun invalidateParent() {
		if (parent?.validParents == true) parent?.invalidate()
		validParents = false
	}

	open fun render(rc: SceneRenderContext) {
	}

	fun removeFromParent() = run { parent?.removeChild(this) }

	open operator fun get(name: String): View? = if (this.name == name) this else null

	open fun clone(): View = View().apply { copyPropertiesFrom(this@View) }
	open fun copyPropertiesFrom(other: View) {
		x = other.x
		y = other.y
		scaleX = other.scaleX
		scaleY = other.scaleY
		rotation = other.rotation
		alpha = other.alpha
		speed = other.speed
	}
}

class ViewTransform {
	val matrix = Matrix2d()
	val transform = Matrix2d.Transform()

	fun update() {
		transform.toMatrix(matrix)
	}
}

open class ViewContainer : View() {
	private val _children = arrayListOf<View>()
	val children: List<View> get() = _children

	fun removeChild(view: View) {
		if (view.parent == this) {
			view.parent = null
			_children.remove(view)
		}
	}

	fun removeChildren() {
		while (children.isNotEmpty()) removeChild(children.last())
	}

	fun addChild(view: View) {
		if (view == this) throw RuntimeException("Can't add view to itself!")
		view.removeFromParent()
		view.parent = this
		this._children += view
		view.invalidate()
	}

	override fun render(rc: SceneRenderContext) {
		for (child in _children) child.render(rc)
	}

	operator fun plusAssign(view: View) {
		addChild(view)
	}

	override fun invalidateChildren() {
		validChildren = false
		for (child in _children) {
			if (child.validChildren) child.invalidateChildren()
		}
	}

	override fun viewInGlobal(x: Double, y: Double): View? {
		for (child in _children) return child.viewInGlobal(x, y) ?: continue
		return null
	}

	override operator fun get(name: String): View? {
		if (this.name == name) return this
		for (c in children) return c[name] ?: continue
		return null
	}

	override fun clone(): ViewContainer {
		val out = ViewContainer()
		out.copyPropertiesFrom(this)
		for (child in children) out.addChild(child.clone())
		return out
	}
}


open class Image(var tex: SceneTexture) : View() {
	var computedAlpha = 1.0

	private val quad = Quad()
	val width get() = tex.width.toDouble()
	val height get() = tex.height.toDouble()

	var anchorX: Double = 0.0
	var anchorY: Double = 0.0
	fun anchor(ax: Double, ay: Double = ax) = run { anchorX = ax; anchorY = ay }

	override fun recompute() {
		if (validParents && validChildren) return
		super.recompute()
		val gm = _globalMatrix
		val sx = -width * anchorX
		val sy = -height * anchorY
		quad.set(gm, sx, sy, width, height)
		computedAlpha = concatAlpha
	}

	override fun viewInGlobal(x: Double, y: Double): View? {
		val localX = globalToLocalX(x, y)
		val localY = globalToLocalY(x, y)
		val sx = -width * anchorX
		val sy = -height * anchorY
		return if (localX >= sx && localX <= sx + width && localY >= sy && localY <= sy + height) this else null
	}

	override fun render(rc: SceneRenderContext) {
		recompute()
		rc.batcher.addQuad(quad, tex, computedAlpha.toFloat())
	}

	override fun clone(): Image = Image(tex).apply { copyPropertiesFrom(this@Image) }
}

open class Text(initialFont: BitmapFont, initialText: String, initialSize: Int = 32) : View() {
	var font: BitmapFont = initialFont
		set(value) {
			field = value
			invalidate()
		}
	var text: String = initialText
		set(value) {
			field = value
			invalidate()
		}

	var size: Int = initialSize
		set(value) {
			field = value
			invalidate()
		}

	private val quads = arrayListOf<QuadWithTexture>()
	private val localBounds = Rectangle()
	private var computedAlpha = 1.0
	private val _tm = Matrix2d()

	var anchorX: Double = 0.0
	var anchorY: Double = 0.0
	fun anchor(ax: Double, ay: Double = ax) = run { anchorX = ax; anchorY = ay }

	override fun recompute() {
		if (validParents && validChildren) return
		super.recompute()
		_tm.copyFrom(_globalMatrix)
		val scale = size.toDouble() / font.lineHeight.toDouble()
		_tm.prescale(scale, scale)
		quads.clear()
		font.renderQuads(_tm, text, 0.0, 0.0, localBounds, null)
		val sx2 = -localBounds.width * anchorX
		val sy2 = -localBounds.height * anchorY
		font.renderQuads(_tm, text, sx2, sy2, localBounds, quads)
		localBounds.width *= scale
		localBounds.height *= scale
		//println("FONT: size=$size, lineHeight=${font.lineHeight}, scale=$scale")
		computedAlpha = concatAlpha
		//println(localBounds)
	}

	override fun viewInGlobal(x: Double, y: Double): View? {
		val localX = globalToLocalX(x, y)
		val localY = globalToLocalY(x, y)
		return if (localX >= localBounds.left && localX <= localBounds.right && localY >= localBounds.top && localY <= localBounds.bottom) this else null
	}

	override fun render(rc: SceneRenderContext) {
		recompute()
		for (quad in quads) {
			rc.batcher.addQuad(quad.quad, quad.tex, computedAlpha.toFloat())
		}
	}
}
