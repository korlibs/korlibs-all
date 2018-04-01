package com.soywiz.korte

import com.soywiz.korio.lang.Charset
import com.soywiz.korio.lang.Charsets

class TemplateConfig(
	extraTags: List<Tag> = listOf(),
	extraFilters: List<Filter> = listOf(),
	extraFunctions: List<TeFunction> = listOf(),
	val charset: Charset = Charsets.UTF_8
) {
	val integratedFunctions = DefaultFunctions.ALL
	val integratedFilters = DefaultFilters.ALL
	val integratedTags = DefaultTags.ALL

	private val allFunctions = integratedFunctions + extraFunctions
	private val allTags = integratedTags + extraTags
	private val allFilters = integratedFilters + extraFilters

	val tags = hashMapOf<String, Tag>().apply {
		for (tag in allTags) {
			this[tag.name] = tag
			for (alias in tag.aliases) this[alias] = tag
		}
	}

	val filters = hashMapOf<String, Filter>().apply {
		for (filter in allFilters) this[filter.name] = filter
	}

	val functions = hashMapOf<String, TeFunction>().apply {
		for (func in allFunctions) this[func.name] = func
	}
}