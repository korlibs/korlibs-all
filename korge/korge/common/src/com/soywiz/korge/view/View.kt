package com.soywiz.korge.view

import com.soywiz.kds.*
import com.soywiz.korge.component.*
import com.soywiz.korge.render.*
import com.soywiz.korim.color.*
import com.soywiz.korio.error.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import com.soywiz.korma.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.event.*
import kotlin.collections.ArrayList
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.arrayListOf
import kotlin.collections.contains
import kotlin.collections.firstOrNull
import kotlin.collections.iterator
import kotlin.collections.joinToString
import kotlin.collections.linkedMapOf
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.plusAssign
import kotlin.collections.removeAll
import kotlin.collections.set
import kotlin.reflect.*

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class ViewsDslMarker

typealias DisplayObject = View

abstract class View : Renderable, Extra by Extra.Mixin(), EventDispatcher by EventDispatcher.Mixin() {
	/**
	 * Views marked with this, break batching by acting as reference point for computing vertices.
	 * Specially useful for containers whose most of their child are less likely to change but the container
	 * itself is going to change like cameras, viewports and the Stage.
	 */
	interface Reference // View that breaks batching Viewport

	enum class HitTestType {
		BOUNDING, SHAPE
	}

	companion object {
		fun commonAncestor(left: View?, right: View?): View? {
			var l: View? = left
			var r: View? = right
			var lCount = l.ancestorCount
			var rCount = r.ancestorCount
			while (lCount != rCount) {
				if (lCount > rCount) {
					lCount--
					l = l?.parent
				} else {
					rCount--
					r = r?.parent
				}
				if (lCount < 0 && rCount < 0) break
			}
			return if (l == r) l else null
		}
	}

	open var ratio: Double = 0.0
	var index: Int = 0; internal set
	var speed: Double = 1.0
	var parent: Container? = null; internal set
	var name: String? = null
	var blendMode: BlendMode = BlendMode.INHERIT
		set(value) {
			if (field != value) {
				field = value
				invalidate()
			}
		}

	val globalSpeed: Double get() = if (parent != null) parent!!.globalSpeed * speed else speed

	var _computedBlendMode: BlendMode = BlendMode.INHERIT

	// @TODO: Cache results
	val computedBlendMode: BlendMode
		get() {
			_ensureGlobal()
			return _computedBlendMode
		}

	private var _scaleX: Double = 1.0
	private var _scaleY: Double = 1.0
	private var _skewX: Double = 0.0
	private var _skewY: Double = 0.0
	private var _rotation: Double = 0.0

	val pos = MPoint2d()

	var x: Double
		get() = ensureTransform().pos.x
		set(v) = run {
			ensureTransform(); if (pos.x != v) run { pos.x = v; invalidateMatrix() }
		}

	var y: Double
		set(v) = run {
			ensureTransform(); if (pos.y != v) run { pos.y = v; invalidateMatrix() }
		}
		get() = ensureTransform().pos.y

	var scaleX: Double
		set(v) = run {
			ensureTransform(); if (_scaleX != v) run { _scaleX = v; invalidateMatrix() }
		}
		get() = ensureTransform()._scaleX

	var scaleY: Double
		set(v) = run {
			ensureTransform(); if (_scaleY != v) run { _scaleY = v; invalidateMatrix() }
		}
		get() = ensureTransform()._scaleY

	var skewX: Double
		set(v) = run {
			ensureTransform(); if (_skewX != v) run { _skewX = v; invalidateMatrix() }
		}
		get() = ensureTransform()._skewX

	var skewY: Double
		set(v) = run {
			ensureTransform(); if (_skewY != v) run { _skewY = v; invalidateMatrix() }
		}
		get() = ensureTransform()._skewY

	var rotation: Double
		set(v) = run {
			ensureTransform(); if (_rotation != v) run { _rotation = v; invalidateMatrix() }
		}
		get() = ensureTransform()._rotation

	var rotationDegrees: Double; set(v) = run { rotation = Angle.toRadians(v) }; get() = Angle.toDegrees(rotation)
	var scale: Double; get() = (scaleX + scaleY) / 2.0; set(v) = run { scaleX = v; scaleY = v }
	var globalX: Double
		get() = parent?.localToGlobalX(x, y) ?: x;
		set(value) = run {
			x = parent?.globalToLocalX(
				value,
				globalY
			) ?: value
		}
	var globalY: Double
		get() = parent?.localToGlobalY(x, y) ?: y;
		set(value) = run { y = parent?.globalToLocalY(globalX, value) ?: value }

	fun setSize(width: Double, height: Double) = _setSize(width, true, height, true)

	private fun _setSize(width: Double, swidth: Boolean, height: Double, sheight: Boolean) {
		//val bounds = parent?.getLocalBounds() ?: this.getLocalBounds()
		val bounds = this.getLocalBounds()
		if (swidth) scaleX = width / bounds.width
		if (sheight) scaleY = height / bounds.height
	}

	open var width: Double
		get() = getLocalBounds().width * scaleX
		set(value) {
			_setSize(value, true, 0.0, false)
		}

	open var height: Double
		get() = getLocalBounds().height * scaleY
		set(value) {
			_setSize(0.0, false, value, true)
		}

	private val _colorTransform = ColorTransform()
	private var _globalColorTransform = ColorTransform()

	var colorMul: RGBA
		get() = _colorTransform.colorMul;
		set(v) = run {
			_colorTransform.colorMul = v; invalidateColorTransform()
		}
	var colorAdd: Int
		get() = _colorTransform.colorAdd;
		set(v) = run {
			_colorTransform.colorAdd = v; invalidateColorTransform()
		}
	var alpha: Double get() = _colorTransform.mA; set(v) = run { _colorTransform.mA = v;invalidateColorTransform() }
	var colorTransform: ColorTransform get() = _colorTransform; set(v) = run { _colorTransform.copyFrom(v); invalidateColorTransform() }

	// alias
	var tint: RGBA
		get() = colorMul
		set(value) = run { colorMul = value }

	private fun invalidateColorTransform() {
		invalidate()
	}

	// region Properties
	private val _props = linkedMapOf<String, String>()
	val props: Map<String, String> get() = _props

	fun hasProp(key: String) = key in _props
	fun getPropString(key: String, default: String = "") = _props[key] ?: default
	fun getPropInt(key: String, default: Int = 0) = _props[key]?.toIntOrNull() ?: default
	fun getPropDouble(key: String, default: Double = 0.0) = _props[key]?.toDoubleOrNull() ?: default

	fun addProp(key: String, value: String) {
		_props[key] = value
		//val componentGen = views.propsTriggers[key]
		//if (componentGen != null) {
		//	componentGen(this, key, value)
		//}
	}

	fun addProps(values: Map<String, String>) {
		for (pair in values) addProp(pair.key, pair.value)
	}
	// endregion

	private val tempTransform = Matrix2d.Transform()
	//private val tempMatrix = Matrix2d()

	private fun ensureTransform() = this.apply {
		if (!validLocalProps) {
			validLocalProps = true
			val t = tempTransform.setMatrix(this._localMatrix)
			this.pos.x = t.x
			this.pos.y = t.y
			this._scaleX = t.scaleX
			this._scaleY = t.scaleY
			this._skewX = t.skewX
			this._skewY = t.skewY
			this._rotation = t.rotation
		}
	}

	val root: View get() = parent?.root ?: this
	val stage: Stage? get() = root as? Stage?

	var mouseEnabled: Boolean = true
	//var mouseChildren: Boolean = false
	var enabled: Boolean = true
	var visible: Boolean = true

	fun setMatrix(matrix: Matrix2d) {
		this._localMatrix.copyFrom(matrix)
		this.validLocalProps = false
	}

	fun setMatrixInterpolated(ratio: Double, l: Matrix2d, r: Matrix2d) {
		this._localMatrix.setToInterpolated(ratio, l, r)
		this.validLocalProps = false
	}

	fun setComputedTransform(transform: Matrix2d.Computed) {
		val m = transform.matrix
		val t = transform.transform
		_localMatrix.copyFrom(m)
		pos.x = t.x; pos.y = t.y
		_scaleX = t.scaleX; _scaleY = t.scaleY
		_skewX = t.skewY; _skewY = t.skewY
		_rotation = t.rotation
		validLocalProps = true
		validLocalMatrix = true
		invalidate()
	}

	fun setTransform(transform: Matrix2d.Transform) {
		val t = transform
		//transform.toMatrix(_localMatrix)
		pos.x = t.x; pos.y = t.y
		_scaleX = t.scaleX; _scaleY = t.scaleY
		_skewX = t.skewY; _skewY = t.skewY
		_rotation = t.rotation
		validLocalProps = true
		validLocalMatrix = false
		invalidate()
	}

	fun setTransform(x: Double, y: Double, sx: Double, sy: Double, angle: Double, skewX: Double, skewY: Double, pivotX: Double = 0.0, pivotY: Double = 0.0) =
		setTransform(tempTransform.setTo(x, y, sx, sy, skewX, skewY, rotation))

	private var _localMatrix = Matrix2d()
	var _globalMatrix = Matrix2d()
	private var _globalVersion = 0
	private var _globalMatrixInvVersion = 0
	private var _globalMatrixInv = Matrix2d()

	internal var validLocalProps = true
	internal var validLocalMatrix = true
	internal var validGlobal = false

	val unsafeListRawComponents get() = components

// region Components
	private var components: ArrayList<Component>? = null
	private var _componentsIt: ArrayList<Component>? = null
	private val componentsIt: ArrayList<Component>?
		get() {
			if (components != null) {
				if (_componentsIt == null) _componentsIt = ArrayList()
				_componentsIt!!.clear()
				_componentsIt!!.addAll(components!!)
			}
			return _componentsIt
		}

	inline fun <reified T : Component> getOrCreateComponent(noinline gen: (View) -> T): T =
		getOrCreateComponent(T::class, gen)

	fun removeComponent(c: Component): Unit {
		//println("Remove component $c from $this")
		components?.remove(c)
	}

	//fun removeComponents(c: KClass<out Component>) = run { components?.removeAll { it.javaClass.isSubtypeOf(c) } }
	fun removeComponents(c: KClass<out Component>) = run {
		//println("Remove components of type $c from $this")
		components?.removeAll { it::class == c }
	}

	fun removeAllComponents() = run {
		components?.clear()
	}

	fun addComponent(c: Component): Component {
		if (components == null) components = arrayListOf()
		components?.plusAssign(c)
		return c
	}

	fun addUpdatable(updatable: (dtMs: Int) -> Unit): Cancellable {
		val component = object : UpdateComponent {
			override val view: View get() = this@View
			override fun update(ms: Double) = run { updatable(ms.toInt()) }
		}.attach()
		component.update(0.0)
		return Cancellable { component.detach() }
	}

	fun <T : Component> getOrCreateComponent(clazz: KClass<T>, gen: (View) -> T): T {
		if (components == null) components = arrayListOf()
		//var component = components!!.firstOrNull { it::class.isSubtypeOf(clazz) }
		var component = components!!.firstOrNull { it::class == clazz }
		if (component == null) {
			component = gen(this)
			components!! += component
		}
		return component!! as T
	}
// endregion

	var localMatrix: Matrix2d
		get() {
			if (validLocalMatrix) return _localMatrix
			validLocalMatrix = true
			_localMatrix.setTransform(x, y, scaleX, scaleY, rotation, skewX, skewY)
			return _localMatrix
		}
		set(value) {
			setMatrix(value)
			invalidate()
		}

	private fun _ensureGlobal() = this.apply {
		if (validGlobal) return@apply
		validGlobal = true
		if (parent != null) {
			_globalMatrix.multiply(localMatrix, parent!!.globalMatrix)
			_globalColorTransform.setToConcat(_colorTransform, parent!!.globalColorTransform)
			_computedBlendMode = if (blendMode == BlendMode.INHERIT) parent!!.computedBlendMode else blendMode
		} else {
			_globalMatrix.copyFrom(localMatrix)
			_globalColorTransform.copyFrom(_colorTransform)
			_computedBlendMode = if (blendMode == BlendMode.INHERIT) BlendMode.NORMAL else blendMode
		}
		_globalVersion++
	}

	var globalMatrix: Matrix2d
		get() = _ensureGlobal()._globalMatrix
		set(value) {
			if (parent != null) {
				this.localMatrix.multiply(value, parent!!.globalMatrixInv)
			} else {
				this.localMatrix.copyFrom(value)
			}
		}

	// @TODO: Use matrix from reference instead
	val renderMatrix: Matrix2d get() = globalMatrix

	val globalColorTransform: ColorTransform get() = run { _ensureGlobal(); _globalColorTransform }
	val globalColorMul: RGBA get() = globalColorTransform.colorMul
	val globalColorAdd: Int get() = globalColorTransform.colorAdd
	val globalAlpha: Double get() = globalColorTransform.mA

	fun localMouseX(views: Views): Double = globalMatrixInv.transformX(views.input.mouse)
	fun localMouseY(views: Views): Double = globalMatrixInv.transformY(views.input.mouse)

	val globalMatrixInv: Matrix2d
		get() {
			_ensureGlobal()
			if (_globalMatrixInvVersion != _globalVersion) {
				_globalMatrixInvVersion = _globalVersion
				_globalMatrixInv.setToInverse(_globalMatrix)
			}
			return _globalMatrixInv
		}

	fun invalidateMatrix() {
		validLocalMatrix = false
		invalidate()
	}

	protected var dirtyVertices = false

	open fun invalidate() {
		validGlobal = false
		dirtyVertices = true
	}

	abstract override fun render(ctx: RenderContext)

	@Suppress("RemoveCurlyBracesFromTemplate")
	override fun toString(): String {
		var out = this::class.portableSimpleName
		if (x != 0.0 || y != 0.0) out += ":pos=(${x.str},${y.str})"
		if (scaleX != 1.0 || scaleY != 1.0) out += ":scale=(${scaleX.str},${scaleY.str})"
		if (skewX != 0.0 || skewY != 0.0) out += ":skew=(${skewX.str},${skewY.str})"
		if (rotation != 0.0) out += ":rotation=(${rotationDegrees.str}º)"
		if (name != null) out += ":name=($name)"
		if (blendMode != BlendMode.INHERIT) out += ":blendMode=($blendMode)"
		return out
	}

	private val Double.str get() = this.toString(2, skipTrailingZeros = true)

	fun globalToLocal(p: Point2d, out: MPoint2d = MPoint2d()): MPoint2d = globalToLocalXY(p.x, p.y, out)
	fun globalToLocalXY(x: Double, y: Double, out: MPoint2d = MPoint2d()): MPoint2d =
		globalMatrixInv.transform(x, y, out)

	fun globalToLocalX(x: Double, y: Double): Double = globalMatrixInv.transformX(x, y)
	fun globalToLocalY(x: Double, y: Double): Double = globalMatrixInv.transformY(x, y)

	fun localToGlobal(p: Point2d, out: MPoint2d = MPoint2d()): MPoint2d = localToGlobalXY(p.x, p.y, out)
	fun localToGlobalXY(x: Double, y: Double, out: MPoint2d = MPoint2d()): MPoint2d = globalMatrix.transform(x, y, out)
	fun localToGlobalX(x: Double, y: Double): Double = globalMatrix.transformX(x, y)
	fun localToGlobalY(x: Double, y: Double): Double = globalMatrix.transformY(x, y)

	fun hitTest(x: Double, y: Double, type: HitTestType): View? = when (type) {
		HitTestType.SHAPE -> hitTest(x, y)
		HitTestType.BOUNDING -> hitTestBounding(x, y)
	}

	fun hitTest(pos: Point2d): View? = hitTest(pos.x, pos.y)

	fun hitTest(x: Double, y: Double): View? {
		if (!mouseEnabled) return null
		return hitTestInternal(x, y)
	}

	fun hitTestBounding(x: Double, y: Double): View? {
		if (!mouseEnabled) return null
		return hitTestBoundingInternal(x, y)
	}

	open fun hitTestInternal(x: Double, y: Double): View? {
		val bounds = getLocalBounds()
		val sLeft = bounds.left
		val sTop = bounds.top
		val sRight = bounds.right
		val sBottom = bounds.bottom
		return if (checkGlobalBounds(x, y, sLeft, sTop, sRight, sBottom)) this else null
	}

	open fun hitTestBoundingInternal(x: Double, y: Double): View? {
		val bounds = getGlobalBounds()
		return if (bounds.contains(x, y)) this else null
	}

	protected fun checkGlobalBounds(
		x: Double,
		y: Double,
		sLeft: Double,
		sTop: Double,
		sRight: Double,
		sBottom: Double
	): Boolean {
		val lx = globalToLocalX(x, y)
		val ly = globalToLocalY(x, y)
		return lx >= sLeft && ly >= sTop && lx < sRight && ly < sBottom
	}

	open fun reset() {
		_localMatrix.setToIdentity()
		pos.setTo(0.0, 0.0)
		_scaleX = 1.0; _scaleY = 1.0
		_skewX = 0.0; _skewY = 0.0
		_rotation = 0.0
		validLocalMatrix = false
		validGlobal = false
		invalidate()
	}

	fun removeFromParent() {
		if (parent == null) return
		val p = parent!!
		for (i in index + 1 until p.children.size) p.children[i].index--
		p.children.removeAt(index)
		parent = null
		index = -1
	}

	//fun getConcatMatrix(target: View, out: Matrix2d = Matrix2d()): Matrix2d {
	//	var current: View? = this
	//	out.setToIdentity()
	//
	//	val views = arrayListOf<View>()
	//	while (current != null) {
	//		views += current
	//		if (current == target) break
	//		current = current.parent
	//	}
	//	for (view in views.reversed()) out.premultiply(view.localMatrix)
	//
	//	return out
	//}

	fun getConcatMatrix(target: View, out: Matrix2d = Matrix2d()): Matrix2d {
		var current: View? = this
		out.setToIdentity()

		while (current != null) {
			//out.premultiply(current.localMatrix)
			out.multiply(out, current.localMatrix)
			if (current == target) break
			current = current.parent
		}

		return out
	}

	val globalBounds: Rectangle get() = getGlobalBounds()
	fun getGlobalBounds(out: Rectangle = Rectangle()): Rectangle = getBounds(this.root, out)

	fun getBounds(target: View? = this, out: Rectangle = Rectangle()): Rectangle {
		//val concat = (parent ?: this).getConcatMatrix(target ?: this)
		val concat = (this).getConcatMatrix(target ?: this)
		val bb = BoundsBuilder()

		getLocalBoundsInternal(out)

		val p1 = Point2d(out.left, out.top)
		val p2 = Point2d(out.right, out.top)
		val p3 = Point2d(out.right, out.bottom)
		val p4 = Point2d(out.left, out.bottom)

		bb.add(concat.transformX(p1.x, p1.y), concat.transformY(p1.x, p1.y))
		bb.add(concat.transformX(p2.x, p2.y), concat.transformY(p2.x, p2.y))
		bb.add(concat.transformX(p3.x, p3.y), concat.transformY(p3.x, p3.y))
		bb.add(concat.transformX(p4.x, p4.y), concat.transformY(p4.x, p4.y))

		bb.getBounds(out)
		return out
	}

	fun getLocalBounds(out: Rectangle = Rectangle()) = out.apply { getLocalBoundsInternal(out) }

	open fun getLocalBoundsInternal(out: Rectangle = Rectangle()): Unit = run { out.setTo(0, 0, 0, 0) }

	protected open fun createInstance(): View =
		throw MustOverrideException("Must Override ${this::class}.createInstance()")

	open fun copyPropsFrom(source: View) {
		this.name = source.name
		this.colorAdd = source.colorAdd
		this.colorMul = source.colorMul
		this.setMatrix(source.localMatrix)
		this.visible = source.visible
		this.ratio = source.ratio
		this.speed = source.speed
		this.blendMode = source.blendMode
	}

	fun findViewByName(name: String): View? {
		if (this.name == name) return this
		if (this is Container) {
			for (child in children) {
				val named = child.findViewByName(name)
				if (named != null) return named
			}
		}
		return null
	}

	open fun clone(): View = createInstance().apply {
		this@apply.copyPropsFrom(this@View)
	}
}

open class DummyView : View() {
	override fun createInstance(): View = DummyView()
	override fun render(ctx: RenderContext) = Unit
}

fun View.hasAncestor(ancestor: View): Boolean {
	return if (this == ancestor) true else this.parent?.hasAncestor(ancestor) ?: false
}

fun View.replaceWith(view: View): Boolean {
	if (this == view) return false
	if (parent == null) return false
	view.parent?.children?.remove(view)
	parent!!.children[this.index] = view
	view.index = this.index
	view.parent = parent
	parent = null
	view.invalidate()
	this.index = -1
	return true
}

val View?.ancestorCount: Int get() = this?.parent?.ancestorCount?.plus(1) ?: 0

fun View?.ancestorsUpTo(target: View?): List<View> {
	var current = this
	val out = arrayListOf<View>()
	while (current != null && current != target) {
		out += current
		current = current.parent
	}
	return out
}

val View?.ancestors: List<View> get() = ancestorsUpTo(null)

fun View?.dump(views: Views, emit: (String) -> Unit = ::println) {
	if (this != null) views.dumpView(this, emit)
}

fun View?.dumpToString(views: Views): String {
	if (this == null) return ""
	val out = arrayListOf<String>()
	dump(views) { out += it }
	return out.joinToString("\n")
}

fun View?.foreachDescendant(handler: (View) -> Unit) {
	if (this != null) {
		handler(this)
		if (this is Container) {
			for (child in this.children) {
				child.foreachDescendant(handler)
			}
		}
	}
}

fun View?.descendantsWithProp(prop: String, value: String? = null): List<View> {
	if (this == null) return listOf()
	return this.descendantsWith {
		if (value != null) {
			it.props[prop] == value
		} else {
			prop in it.props
		}
	}
}

fun View?.descendantsWithPropString(prop: String, value: String? = null): List<Pair<View, String>> =
	this.descendantsWithProp(prop, value).map { it to it.getPropString(prop) }

fun View?.descendantsWithPropInt(prop: String, value: Int? = null): List<Pair<View, Int>> =
	this.descendantsWithProp(prop, if (value != null) "$value" else null).map { it to it.getPropInt(prop) }

fun View?.descendantsWithPropDouble(prop: String, value: Double? = null): List<Pair<View, Int>> =
	this.descendantsWithProp(prop, if (value != null) "$value" else null).map { it to it.getPropInt(prop) }

operator fun View?.get(name: String): View? = firstDescendantWith { it.name == name }

@Deprecated("", ReplaceWith("this[name]", "com.soywiz.korge.view.get"))
fun View?.firstDescendantWithName(name: String): View? = this[name]

val View?.allDescendantNames
	get(): List<String> {
		val out = arrayListOf<String>()
		foreachDescendant {
			if (it.name != null) out += it.name!!
		}
		return out
	}

fun View?.firstDescendantWith(check: (View) -> Boolean): View? {
	if (this == null) return null
	if (check(this)) return this
	if (this is Container) {
		for (child in this.children) {
			val res = child.firstDescendantWith(check)
			if (res != null) return res
		}
	}
	return null
}

fun View?.descendantsWith(out: ArrayList<View> = arrayListOf(), check: (View) -> Boolean): List<View> {
	if (this != null) {
		if (check(this)) out += this
		if (this is Container) {
			for (child in this.children) {
				child.descendantsWith(out, check)
			}
		}
	}
	return out
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T : View> T.xy(x: Number, y: Number): T =
	this.apply { this.x = x.toDouble() }.apply { this.y = y.toDouble() }

inline fun <T : View> T.position(x: Number, y: Number): T =
	this.apply { this.x = x.toDouble() }.apply { this.y = y.toDouble() }

inline fun <T : View> T.rotation(rot: Angle): T =
	this.apply { this.rotation = rot.radians }

inline fun <T : View> T.rotation(rot: Number): T =
	this.apply { this.rotation = rot.toDouble() }

inline fun <T : View> T.rotationDegrees(degs: Number): T =
	this.apply { this.rotationDegrees = degs.toDouble() }

inline fun <T : View> T.skew(sx: Number, sy: Number): T =
	this.apply { this.skewX = sx.toDouble() }.apply { this.skewY = sy.toDouble() }

inline fun <T : View> T.scale(sx: Number, sy: Number = sx): T =
	this.apply { this.scaleX = sx.toDouble() }.apply { this.scaleY = sy.toDouble() }

inline fun <T : View> T.alpha(alpha: Number): T =
	this.apply { this.alpha = alpha.toDouble() }

