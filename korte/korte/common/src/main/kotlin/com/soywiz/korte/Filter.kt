package com.soywiz.korte

data class Filter(val name: String, val eval: suspend (subject: Any?, args: List<Any?>, context: Template.EvalContext) -> Any?) {
}
