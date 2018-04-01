package com.soywiz.korte

@Suppress("unused")
object DefaultTags {
	val BlockTag = Tag("block", setOf(), setOf("end", "endblock")) {
		val part = chunks.first()
		val tokens = ExprNode.Token.tokenize(part.tag.content)
		val name = ExprNode.parseId(tokens)
		if (name.isEmpty()) throw IllegalArgumentException("block without name")
		context.template.addBlock(name, part.body)
		DefaultBlocks.BlockBlock(name)
	}

	val Capture = Tag("capture", setOf(), null) {
		val main = chunks[0]
		val tr = ExprNode.Token.tokenize(main.tag.content)
		val varname = ExprNode.parseId(tr)
		DefaultBlocks.BlockCapture(varname, main.body)
	}

	val Debug = Tag("debug", setOf(), null) {
		DefaultBlocks.BlockDebug(ExprNode.parse(chunks[0].tag.content))
	}

	val Empty = Tag("", setOf(""), null) {
		Block.group(chunks.map { it.body })
	}

	val Extends = Tag("extends", setOf(), null) {
		val part = chunks.first()
		val parent = ExprNode.parseExpr(ExprNode.Token.tokenize(part.tag.content))
		DefaultBlocks.BlockExtends(parent)
	}

	val For = Tag("for", setOf("else"), setOf("end", "endfor")) {
		val main = chunks[0]
		val elseTag = chunks.getOrNull(1)?.body
		val tr = ExprNode.Token.tokenize(main.tag.content)
		val varnames = arrayListOf<String>()
		do {
			varnames += ExprNode.parseId(tr)
		} while (tr.tryRead(",") != null)
		ExprNode.expect(tr, "in")
		val expr = ExprNode.parseExpr(tr)
		DefaultBlocks.BlockFor(varnames, expr, main.body, elseTag)
	}

	val If = Tag("if", setOf("else", "elseif"), setOf("end", "endif")) {
		val ifBranches = arrayListOf<Pair<ExprNode, Block>>()
		var elseBranch: Block? = null

		for (part in chunks) {
			when (part.tag.name) {
				"if", "elseif" -> {
					ifBranches += ExprNode.parse(part.tag.content) to part.body
				}
				"else" -> {
					elseBranch = part.body
				}
			}
		}
		val ifBranchesRev = ifBranches.reversed()
		var node: Block = DefaultBlocks.BlockIf(ifBranchesRev.first().first, ifBranchesRev.first().second, elseBranch)
		for (branch in ifBranchesRev.takeLast(ifBranchesRev.size - 1)) {
			node = DefaultBlocks.BlockIf(branch.first, branch.second, node)
		}

		node
	}

	val Import = Tag("import", setOf(), null) {
		val part = chunks.first()
		val s = ExprNode.Token.tokenize(part.tag.content)
		val file = s.parseExpr()
		s.expect("as")
		val name = s.read().text
		DefaultBlocks.BlockImport(file, name)
	}

	val Include = Tag("include", setOf(), null) {
		val part = chunks.first()
		val fileName = ExprNode.parseExpr(part.tag.tokens)
		DefaultBlocks.BlockInclude(fileName)
	}

	val Macro = Tag("macro", setOf(), setOf("end", "endmacro")) {
		val part = chunks[0]
		val s = ExprNode.Token.tokenize(part.tag.content)
		val funcname = s.parseId()
		s.expect("(")
		val params = s.parseIdList()
		s.expect(")")
		DefaultBlocks.BlockMacro(funcname, params, part.body)
	}

	val Set = Tag("set", setOf(), null) {
		val main = chunks[0]
		val tr = ExprNode.Token.tokenize(main.tag.content)
		val varname = ExprNode.parseId(tr)
		ExprNode.expect(tr, "=")
		val expr = ExprNode.parseExpr(tr)
		DefaultBlocks.BlockSet(varname, expr)
	}

	val ALL = listOf(
		BlockTag,
		Capture, Debug,
		Empty, Extends, For, If, Import, Include, Macro, Set
	)
}