package example2

import com.soywiz.kds.*
import com.soywiz.korui.*
import com.soywiz.korui.light.*
import com.soywiz.korui.react.*
import com.soywiz.korui.ui.*
import kotlinx.coroutines.experimental.*

object Example2Jvm {
    @JvmStatic
    fun main(args: Array<String>) = example2.main(args)
}

/*
object Experiment {
    @JvmStatic
    fun main(args: Array<String>) {
        val app = Application(DefaultDispatcher, DummyLightComponents())
        val container1 = Container(app, LayeredLayout(app))
        container1.attachReactComponent(ReactApp(), MyState(count = 1))
        val container2 = Container(app, LayeredLayout(app))
        container2.attachReactComponent(ReactApp(), MyState(count = 2))
        container1.synchronizeTo(container2)
    }
}
*/
