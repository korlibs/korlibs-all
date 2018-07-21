package example2

import com.soywiz.korim.vector.chart.*
import com.soywiz.korui.*
import com.soywiz.korui.event.*
import com.soywiz.korui.geom.len.*
import com.soywiz.korui.react.*
import com.soywiz.korui.style.*
import com.soywiz.korui.ui.*

fun main(args: Array<String>) = Application {
    //Logger.defaultLevel = Logger.Level.TRACE
    //Logger("korui-application").level = Logger.Level.TRACE
    reactFrame(ReactApp(), MyState(count = 0), "HELLO")

}

data class MyState(val count: Int = 0, val slider: Int = 50)

class ReactApp : ReactComponent<MyState>() {
    // @TODO: VirtualDOM (to prevent regenerating slider)
    override suspend fun Container.render() {
        tabPane {
            page("Main") {
                scrollPane {
                    vertical {
                        inline {
                            label("Count: ${state.count}") {
                            }
                            button("Click") {
                                mouse {
                                    click {
                                        println("ON CLICK")
                                        state = state.copy(count = state.count + 1)
                                    }
                                }
                            }
                            slider(state.slider, 100) {
                                key = "CountSlider"
                                onUpdate {
                                    println("Updated $it, ${this.current}")
                                    state = state.copy(slider = current)
                                }
                            }
                        }
                        inline {
                            vectorImage(
                                ChartBars(
                                    "hello" to 10,
                                    "world" to 20 + state.count * 3,
                                    "this" to 5,
                                    "is" to 3 + state.count * 5,
                                    "a" to 80,
                                    "slider" to state.slider
                                )
                            ) {
                                width = 512.pt
                                height = 256.pt
                            }
                        }
                        inline {
                            vectorImage(
                                ChartBars(
                                    "hello" to 10,
                                    "world" to 20 + state.count * 3,
                                    "this" to 5,
                                    "is" to 3 + state.count * 5,
                                    "a" to 80,
                                    "slider" to state.slider
                                )
                            ) {
                                width = 512.pt
                                height = 256.pt
                            }
                        }
                    }
                }
            }
            page("Meta") {
                inline {
                    label("Actions:")
                    button("Reset") {
                        mouse {
                            click {
                                state = MyState()
                            }
                        }
                    }
                }
            }
        }
    }
}
