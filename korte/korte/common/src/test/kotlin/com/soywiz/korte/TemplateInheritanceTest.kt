package com.soywiz.korte

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.vfs.MemoryVfsMix
import org.junit.Test
import kotlin.test.assertEquals

class TemplateInheritanceTest {
	@Test
	fun simple() = syncTest {
		assertEquals(
			"hello",
			Templates(MemoryVfsMix(
				"a" to "hello"
			)).get("a").invoke()
		)
	}

	@Test
	fun block() = syncTest {
		assertEquals(
			"hello",
			Templates(MemoryVfsMix(
				"a" to "{% block test %}hello{% end %}"
			)).get("a")()
		)
	}

	@Test
	fun extends() = syncTest {
		val template = Templates(MemoryVfsMix(
			"a" to """{% block test %}a{% end %}""",
			"b" to """{% extends "a" %}{% block test %}b{% end %}"""
		)).get("b")
		assertEquals(
			"b",
			template()
		)
	}

	@Test
	fun doubleExtends() = syncTest {
		assertEquals(
			"c",
			Templates(MemoryVfsMix(
				"a" to """{% block test %}a{% end %}""",
				"b" to """{% extends "a" %}{% block test %}b{% end %}""",
				"c" to """{% extends "b" %}{% block test %}c{% end %}"""
			)).get("c")()
		)
	}

	@Test
	fun blockParent() = syncTest {
		assertEquals(
			"<b><a>TEXT</a></b>",
			Templates(MemoryVfsMix(
				"a" to """{% block test %}<a>TEXT</a>{% end %}""",
				"b" to """{% extends "a" %}{% block test %}<b>{{ parent() }}</b>{% end %}"""
			)).get("b")()
		)
	}

	@Test
	fun blockDoubleParent() = syncTest {
		assertEquals(
			"<c><b><a>TEXT</a></b></c>",
			Templates(MemoryVfsMix(
				"a" to """{% block test %}<a>TEXT</a>{% end %}""",
				"b" to """{% extends "a" %}{% block test %}<b>{{ parent() }}</b>{% end %}""",
				"c" to """{% extends "b" %}{% block test %}<c>{{ parent() }}</c>{% end %}"""
			)).get("c")()
		)
	}

	@Test
	fun nestedBlocks() = syncTest {
		assertEquals(
			"<root><left><L>left:LEFT</L></left><right><R>right:RIGHT</R></right></root>",
			Templates(MemoryVfsMix(
				"root" to """<root>{% block main %}test{% end %}</root>""",
				"2column" to """{% extends "root" %}  {% block main %}<left>{% block left %}left{% end %}</left><right>{% block right %}right{% end %}</right>{% end %}  """,
				"mypage" to """{% extends "2column" %}  {% block right %}<R>{{ parent() }}:RIGHT</R>{% end %}  {% block left %}<L>{{ parent() }}:LEFT</L>{% end %}  """
			)).get("mypage")()
		)
	}

	@Test
	fun doubleExtends2() = syncTest {
		assertEquals(
			"abcc",
			Templates(MemoryVfsMix(
				"a" to """{% block b1 %}a{% end %}{% block b2 %}a{% end %}{% block b3 %}a{% end %}{% block b4 %}a{% end %}""",
				"b" to """{% extends "a" %}{% block b2 %}b{% end %}{% block b4 %}b{% end %}""",
				"c" to """{% extends "b" %}{% block b3 %}c{% end %}{% block b4 %}c{% end %}"""
			)).get("c")()
		)
	}

	@Test
	fun include() = syncTest {
		assertEquals(
			"Hello World, Carlos.",
			Templates(MemoryVfsMix(
				"include" to """World""",
				"username" to """Carlos""",
				"main" to """Hello {% include "include" %}, {% include "username" %}."""
			)).get("main")()
		)
	}

	@Test
	fun jekyllLayout() = syncTest {
		assertEquals(
			"Hello Carlos.",
			Templates(MemoryVfsMix(
				"mylayout" to """Hello {{ content }}.""",
				"main" to """
					---
					layout: mylayout
					name: Carlos
					---
					{{ name }}
				""".trimIndent()
			)).get("main")()
		)
	}

	@Test
	fun jekyllLayoutEx() = syncTest {
		assertEquals(
			"<html><div>side</div><div><h1>Content</h1></div></html>",
			Templates(MemoryVfsMix(
				"root" to """
					<html>{{ content }}</html>
                """.trimIndent(),
				"twocolumns" to """
					---
					layout: root
					---
					<div>side</div><div>{{ content }}</div>
                """.trimIndent(),
				"main" to """
					---
					layout: twocolumns
					mycontent: Content
					---
					<h1>{{ mycontent }}</h1>
				""".trimIndent()
			)).get("main")()
		)
	}

	// @TODO:
	//@Test
	//fun operatorPrecedence() = sync {
	//	Assert.assertEquals("${2 + 3 * 5}", Template("{{ 1 + 2 * 3 }}")())
	//	Assert.assertEquals("${2 * 3 + 5}", Template("{{ 2 * 3 + 5 }}")())
	//}

	@Test
	fun operatorPrecedence() = syncTest {
		assertEquals("true", Template("{{ 1 in [1, 2] }}")())
		assertEquals("false", Template("{{ 3 in [1, 2] }}")())
	}
}