package com.soywiz.kmedialayer.scene.components

import com.soywiz.kmedialayer.scene.*
import com.soywiz.kmedialayer.scene.geom.*
import kotlin.reflect.*

class ActionRunnerComponent(override val view: View) : UpdateComponent {
    val props = ViewActionProperties(view)
    private var runner: Action.Runner? = null
    var elapsedMs = 0.0
    var totalTimeMs = 0.0

    override fun update(ms: Double) {
        if (runner == null || totalTimeMs <= 0.0) return
        elapsedMs += ms
        val ratio = clamp(elapsedMs / totalTimeMs, 0.0, 1.0)
        runner?.update(ratio, set = true)
        if (ratio >= 1.0) {
            runner = null
        }
    }


    fun set(action: Action) {
        if (runner == null) {
            props.reset()
        }
        runner?.update(1.0, set = false)
        elapsedMs = 0.0
        totalTimeMs = action.time * 1000.0
        runner = action.runner(props)
    }
}

class ViewActionProperties(val view: View) {
    open class DestProperty(val prop: KMutableProperty0<Double>) {
        var running = false
        open var initialValue: Double = prop.get(); set(value) = run { field = value }
        open var destValue: Double = prop.get(); set(value) = run { field = value }
        var value get() = prop.get(); set(value) = run { prop.set(value) }
        fun reset() {
            initialValue = prop.get()
            destValue = prop.get()
        }
    }

    class DestPropertyModulo(prop: KMutableProperty0<Double>, val modulo: Double) : DestProperty(prop) {
        override var initialValue: Double = prop.get(); set(value) = run { field = value % modulo }
        override var destValue: Double = prop.get(); set(value) = run { field = value % modulo }
    }

    val x = DestProperty(view::x)
    val y = DestProperty(view::y)
    val alpha = DestProperty(view::alpha)
    //val rotationDegrees = DestPropertyModulo(view::rotationDegrees, modulo = 360.0)
    val rotationDegrees = DestProperty(view::rotationDegrees)
    val props = listOf(x, y, alpha, rotationDegrees)

    fun reset() {
        for (prop in props) prop.reset()
    }
}

interface DoubleProvider {
    fun getValue(time: Double): Double
}

interface Action {
    val time: Double
    fun runner(props: ViewActionProperties): Runner

    abstract class Runner {
        abstract fun update(ratio: Double, set: Boolean)
    }

    open class Property(
        val prop: KMutableProperty1<View, Double>,
        var add: Boolean,
        val value: Double,
        override val time: Double,
        val easing: Easing
    ) : Action {
        override fun runner(props: ViewActionProperties): Runner = object : Runner() {
            val rprop = when (prop) {
                View::x -> props.x
                View::y -> props.y
                View::alpha -> props.alpha
                View::rotationDegrees -> props.rotationDegrees
                else -> throw RuntimeException("Invalid prop $prop")
            }

            var started = false
            override fun update(ratio: Double, set: Boolean) {
                if (!started) {
                    started = true
                    rprop.initialValue = rprop.value
                    if (add) {
                        rprop.destValue += value
                    } else {
                        rprop.destValue = value
                    }
                }
                if (set) {
                    rprop.value = interpolate(easing(ratio), rprop.initialValue, rprop.destValue)
                }
            }
        }
    }

    //class Repeat(count: Int, val action: Action) : Action {
    //    override val time: Double = action.time * count
    //
    //    override fun runner(props: ViewActionProperties): Runner = object : Runner() {
    //        val runners = items.map { it.runner(props) }
    //        override fun update(ratio: Double, set: Boolean) = run { for (runner in runners) runner.update(ratio, set) }
    //    }
    //}

    class Parallel(vararg val items: Action) : Action {
        override val time: Double = items.map { it.time }.max() ?: 0.0

        override fun runner(props: ViewActionProperties): Runner = object : Runner() {
            val runners = items.map { it.runner(props) }
            override fun update(ratio: Double, set: Boolean) = run { for (runner in runners) runner.update(ratio, set) }
        }
    }

    class Sequence(vararg val items: Action) : Action {
        override val time: Double = items.sumByDouble { it.time }

        override fun runner(props: ViewActionProperties): Runner = object : Runner() {
            val runners = items.map { it.runner(props) }
            var currentIndex = -1
            val ratioPerRunner = 1.0 / runners.size.toDouble()
            override fun update(ratio: Double, set: Boolean): Unit = run {
                val index = (runners.size * ratio).toInt()
                val runnerStartRatio = index * ratioPerRunner
                val runnerRatio = (ratio - runnerStartRatio) / ratioPerRunner
                //println("$currentIndex, $index -> $ratio, $runnerStartRatio, $runnerStartRatio, $runnerRatio")
                while (currentIndex < index) {
                    val runner = runners.getOrNull(currentIndex)
                    runner?.update(1.0, set)
                    currentIndex++
                }
                runners.getOrNull(index)?.update(runnerRatio, set)
                Unit
            }
        }
    }
}

fun buildActions(callback: ActionBuilder.() -> Unit): List<Action> = ActionBuilder().apply(callback).run { ActionBuilder.Building.run { buildList() } }
fun buildAction(callback: ActionBuilder.() -> Unit): Action = ActionBuilder().apply(callback).run { ActionBuilder.Building.run { build() } }

class ActionBuilder {
    object Building

    private val list = arrayListOf<Action>()

    private fun actDelta(
        prop: KMutableProperty1<View, Double>,
        delta: Double,
        time: Double = 1.0,
        easing: Easing = Easing.LINEAR
    ): Action = Action.Property(prop, true, delta, time, easing)

    private fun actSet(
        prop: KMutableProperty1<View, Double>,
        value: Double,
        time: Double = 1.0,
        easing: Easing = Easing.LINEAR
    ): Action = Action.Property(prop, false, value, time, easing)

    fun moveBy(x: Double, y: Double, time: Double = 1.0, easing: Easing = Easing.LINEAR) {
        list += Action.Parallel(actDelta(View::x, x, time, easing), actDelta(View::y, y, time, easing))
    }

    fun moveTo(x: Double, y: Double, time: Double = 1.0, easing: Easing = Easing.LINEAR) {
        list += Action.Parallel(actSet(View::x, x, time, easing), actSet(View::y, y, time, easing))
    }

    fun rotateBy(angle: Double, time: Double = 1.0, easing: Easing = Easing.LINEAR) {
        list += actDelta(View::rotationDegrees, angle, time, easing)
    }

    fun sequence(callback: ActionBuilder.() -> Unit) {
        list += Action.Sequence(*buildActions(callback).toTypedArray())
    }

    fun parallel(callback: ActionBuilder.() -> Unit) {
        list += Action.Parallel(*buildActions(callback).toTypedArray())
    }

    fun repeat(count: Int, callback: ActionBuilder.() -> Unit) {
        val action = buildAction(callback)
        list += Action.Sequence(*(0 until count).map { action }.toTypedArray())
    }

    fun Building.buildList(): List<Action> = list
    fun Building.build(): Action = if (list.size == 1) list[0] else Action.Sequence(*list.toTypedArray())
}

/**
 * SpriteKit similar action system
 */
fun View.act(callback: ActionBuilder.() -> Unit) {
    val runner = getOrCreateComponent { ActionRunnerComponent(it) }
    val builder = ActionBuilder()
    callback(builder)
    ActionBuilder.Building.apply {
        builder.apply {
            runner.set(build())
        }
    }
}