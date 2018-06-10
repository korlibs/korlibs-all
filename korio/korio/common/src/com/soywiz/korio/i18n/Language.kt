package com.soywiz.korio.i18n

import com.soywiz.korio.*

// https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes
enum class Language(val iso6391: String, val iso6392: String) {
	JAPANESE("ja", "jpn"),
	ENGLISH("en", "eng"),
	FRENCH("fr", "fra"),
	SPANISH("es", "spa"),
	GERMAN("de", "deu"),
	ITALIAN("it", "ita"),
	DUTCH("nl", "nld"),
	PORTUGUESE("pt", "por"),
	RUSSIAN("ru", "rus"),
	KOREAN("ko", "kor"),
	CHINESE("zh", "zho"),
	;

	companion object {
		val BY_ID = (
				(values().map { it.name.toLowerCase() to it } +
						values().map { it.iso6391 to it } +
						values().map { it.iso6392 to it })
				).toMap()

		val SYSTEM_LANGS = KorioNative.systemLanguageStrings.map { BY_ID[it.split("-").firstOrNull()] }.filterNotNull()
		val SYSTEM = SYSTEM_LANGS.firstOrNull() ?: ENGLISH
		var CURRENT = SYSTEM
	}
}
