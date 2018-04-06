package com.soywiz.dynarek

import kotlin.test.*

class IntegrationTestsJvm {
	@Test
	fun testInterpreter() {
		val function = function(DClass(State::class), DINT, DVOID) {
			SET(p0[State::a], p1 * 2.lit)
		}

		val state = State()
		val interpreter = DSlowInterpreter(listOf(state, 10))
		interpreter.interpret(function)
		assertEquals(20, state.a)
	}

	//fun testWithState(state: State): State {
	//}

	@Test
	fun testDynarek() {
		val function = function(DClass(State::class), DINT, DVOID) {
			SET(p0[State::a], p0[State::a] + 4.lit * p1)
		}
		val state = State(a = 7)
		val func = function.generateDynarek()
		val ret = func(state, 2)

		assertEquals(15, state.a)
	}

	@Test
	fun testDynarek2() {
		val function = function(DClass(State::class), DINT, DVOID) {
			IF(true) {
				SET(p0[State::a], p0[State::a] + 7.lit * p1)
			}

			IF(true) {
				SET(p0[State::a], p0[State::a] + 4.lit * p1)
			} ELSE {
				SET(p0[State::a], 9.lit * p1)
			}

			IF(false) {
				SET(p0[State::a], p0[State::a] + 4.lit * p1)
			} ELSE {
				SET(p0[State::a], p0[State::a] + 11.lit * p1)
			}
		}
		val state = State(a = 7)
		val func = function.generateDynarek()
		val ret = func(state, 3)

		assertEquals(73, state.a)
	}

	@Test
	fun testDynarekInvokeMethod() {
		val function = function(DClass(State::class), DINT, DVOID) {
			STM(State::mulAB.invoke(p0))
		}
		val state = State(a = 7, b = 3)
		function.generateDynarek()(state, 3)

		assertEquals(7 * 3, state.a)
	}

	@Test
	fun testDynarekInvokeMethod2() {
		val function = function(DClass(State::class), DINT, DVOID) {
			STM(State::mulABArg.invoke(p0, 11.lit))
		}
		val state = State(a = 7, b = 3)
		function.generateDynarek()(state, 3)

		assertEquals(7 * 3 * 11, state.a)
	}

	@Test
	fun testDynarekInvokeMethod3() {
		val function = function(DClass(State::class), DINT, DVOID) {
			STM(State::mulABArg2.invoke(p0, 11.lit, 9.lit))
		}
		val state = State(a = 7, b = 3)
		function.generateDynarek()(state, 3)

		assertEquals(7 * 3 * (11 + 9), state.a)
	}

	@Test
	fun testWhile() {
		val function = function(DClass(State::class), DINT, DVOID) {
			WHILE(p0[State::b] gt 0.lit) {
				SET(p0[State::a], p0[State::a] + 1.lit)
				SET(p0[State::b], p0[State::b] - 1.lit)
			}
		}
		val state = State(a = 7, b = 3)
		function.generateDynarek()(state, 3)

		assertEquals(7 + 3, state.a)
	}

	@Test
	fun testImult() {
		val function = function(DClass(State::class), DINT, DVOID) {
			SET(p0[State::a], (Int.MAX_VALUE - 1).lit * (Int.MAX_VALUE - 2).lit)
		}
		val state = State(a = 7, b = 3)
		function.generateDynarek()(state, 3)

		assertEquals(-2147483642, state.a)
	}

	@Test
	fun testIdiv() {
		val function = function(DClass(State::class), DINT, DVOID) {
			SET(p0[State::a], (Int.MAX_VALUE - 1).lit / (Int.MAX_VALUE - 2).lit)
			SET(p0[State::a], (Int.MAX_VALUE - 1).lit % (Int.MAX_VALUE - 2).lit)
		}
		val state = State(a = 7, b = 3)
		function.generateDynarek()(state, 3)

		assertEquals(1, state.a)
		assertEquals(3, state.b)
	}

	@Test
	fun testLocal() {
		val function = function(DClass(State::class), DINT, DVOID) {
			val local1 = DLocal(Int::class, 9.lit)
			SET(local1, local1 * 7.lit)
			SET(p0[State::a], local1)
		}
		val state = State(a = 7, b = 3)
		function.generateDynarek()(state, 3)
		assertEquals(9 * 7, state.a)
	}

	@Test
	fun testFor() {
		val function = function(DClass(State::class), DVOID) {
			val n = DLocal(Int::class, 0.lit)
			FOR(n, start = 3.lit, end = 10.lit) {
				STM(State::log.invoke(p0, -n))
			}
		}
		val state = State()
		function.generateDynarek()(state)

		assertEquals(listOf(-3, -4, -5, -6, -7, -8, -9), state.logList.toList())
	}

	@Test
	fun testBitops() {
		val function = function(DClass(State::class), DVOID) {
			SET(p0[State::a], (1.lit or 2.lit or 4.lit).inv() and 0b011101011.lit)
		}
		val state = State()
		function.generateDynarek()(state)

		assertEquals((1 or 2 or 4).inv() and 0b011101011, state.a)
	}

	@Test
	fun testFloat() {
		val function = function(DClass(State::class), DVOID) {
			SET(p0[State::f0], 1f.lit)
			SET(p0[State::f1], DBinopFloat(p0[State::f0], FBinop.ADD, 3f.lit))
		}
		val state = State()
		function.generateDynarek()(state)

		assertEquals(1f, state.f0)
		assertEquals(4f, state.f1)
	}

	@Test
	fun testRawProperty() {
		val function = function(DClass(State::class), DVOID) {
			SET(p0[State::c], 1.lit)
			SET(p0[State::c], p0[State::c] + 3.lit)
		}
		val state = State()
		function.generateDynarek()(state)

		assertEquals(4, state.c)
	}

	@Test
	fun testGetterSetterProperty() {
		val function = function(DClass(State::class), DVOID) {
			SET(p0[State::d], 1.lit)
			SET(p0[State::d], p0[State::d] + 3.lit)
		}
		val state = State()
		function.generateDynarek()(state)

		assertEquals(4, state.d)
	}

	@Test
	fun testFloatComp() {
		val function = function(DClass(State::class), DVOID) {
			IF(p0[State::_f3] gt p0[State::_fm2]) {
				SET(p0[State::d], 11.lit)
			} ELSE {
				SET(p0[State::d], 12.lit)
			}
		}
		val state = State()
		function.generateDynarek()(state)

		assertEquals(11, state.d)
	}

	@Test
	fun testFloatCompNot() {
		val function = function(DClass(State::class), DVOID) {
			IF((p0[State::_f3] gt p0[State::_fm2]).not()) {
				SET(p0[State::d], 11.lit)
			} ELSE {
				SET(p0[State::d], 12.lit)
			}
		}
		val state = State()
		function.generateDynarek()(state)

		assertEquals(12, state.d)
	}
}
