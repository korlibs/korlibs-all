package com.soywiz.kmedialayer.scene.geom

import kotlin.math.*

class Easing(val func: (Double) -> Double) {
    operator fun invoke(value: Double) = func(value)

    fun Easing.composeWith(other: Easing) = Easing { other(this(it)) }

    companion object {
        val LINEAR = Easing { it }

        val SIN = Easing { sin(it) }

        val QUADRATIC_EASE_IN = Easing { it * it }
        val QUADRATIC_EASE_OUT = Easing { -(it * (it - 2)) }
        val QUADRATIC_EASE_IN_OUT = Easing {
            if (it < 0.5) 2 * it * it else (-2 * it * it) + (4 * it) - 1
        }
        val CUBIC_EASE_IN = Easing { it * it * it }
        val CUBIC_EASE_OUT = Easing { val f = (it - 1); f * f * f + 1 }
        val CUBIC_EASE_IN_OUT = Easing {
            if (it < 0.5) 4 * it * it * it else {
                val f = ((2 * it) - 2)
                0.5 * f * f * f + 1
            }
        }

        val QUARTIC_EASE_IN = Easing { it * it * it * it }
        val QUARTIC_EASE_OUT = Easing { val f = (it - 1); f * f * f * (1 - it) + 1 }
        val QUARTIC_EASE_IN_OUT = Easing {
            if (it < 0.5) 8 * it * it * it * it else {
                val f = (it - 1); -8 * f * f * f * f + 1
            }
        }

        val SINE_EASE_IN = Easing { sin((it - 1.0) * (PI / 2.0)) + 1.0 }
        val SINE_EASE_OUT = Easing { sin(it * (PI / 2.0)) }
        val SINE_EASE_IN_OUT = Easing { 0.5 * (1.0 - cos(it * PI)) }

        val CIRCULAR_EASE_IN = Easing { 1 - sqrt(1 - (it * it)) }
        val CIRCULAR_EASE_OUT = Easing { sqrt((2 - it) * it) }
        val CIRCULAR_EASE_IN_OUT = Easing {
            if (it < 0.5) 0.5 * (1 - sqrt(1 - 4 * (it * it))) else 0.5 * (sqrt(-((2 * it) - 3) * ((2 * it) - 1)) + 1)
        }

        val EXPONENTIAL_EASE_IN = Easing { if (it == 0.0) it else 2.0.pow(10 * (it - 1)) }
        val EXPONENTIAL_EASE_OUT = Easing { if (it == 1.0) it else 1 - 2.0.pow(-10 * it) }
        val EXPONENTIAL_EASE_IN_OUT = Easing {
            if (it == 0.0 || it == 1.0) {
                it
            } else if (it < 0.5) {
                0.5 * 2.0.pow((20 * it) - 10)
            } else {
                -0.5 * 2.0.pow((-20 * it) + 10) + 1
            }
        }

        val ELASTIC_EASE_IN = Easing {
            sin(13 * (PI / 2.0) * it) * 2.0.pow(10.0 * (it - 1.0))
        }

        val ELASTIC_EASE_OUT = Easing {
            sin(-13 * (PI / 2.0) * (it + 1)) * 2.0.pow(-10 * it) + 1
        }

        val ELASTIC_EASE_IN_OUT = Easing {
            if (it < 0.5) {
                0.5 * sin(13.0 * (PI / 2) * (2 * it)) * 2.0.pow(10 * ((2 * it) - 1))
            } else {
                0.5 * (sin(-13 * (PI / 2) * ((2 * it - 1) + 1)) * 2.0.pow(-10 * (2 * it - 1)) + 2)
            }
        }

        val BACK_EASE_IN = Easing { it * it * it - it * sin(it * PI); }
        val BACK_EASE_OUT = Easing { val f = (1 - it); 1 - (f * f * f - f * sin(f * PI)) }

        val BACK_EASE_INOUT = Easing {
            if (it < 0.5) {
                val f = 2 * it
                0.5 * (f * f * f - f * sin(f * PI))
            } else {
                val f = (1 - (2 * it - 1))
                0.5 * (1 - (f * f * f - f * sin(f * PI))) + 0.5
            }
        }

        val BOUNCE_EASE_IN = Easing {
            1 - BOUNCE_EASE_OUT(1 - it)
        }

        val BOUNCE_EASE_OUT = Easing {
            when {
                it < 4 / 11.0 -> (121 * it * it) / 16.0
                it < 8 / 11.0 -> (363 / 40.0 * it * it) - (99 / 10.0 * it) + 17 / 5.0
                it < 9 / 10.0 -> (4356 / 361.0 * it * it) - (35442 / 1805.0 * it) + 16061 / 1805.0
                else -> (54 / 5.0 * it * it) - (513 / 25.0 * it) + 268 / 25.0
            }
        }

        val BOUNCE_EASE_IN_OUT = Easing {
            if (it < 0.5) {
                0.5 * BOUNCE_EASE_IN(it * 2)
            } else {
                0.5 * BOUNCE_EASE_OUT(it * 2 - 1) + 0.5
            }
        }
    }
}
