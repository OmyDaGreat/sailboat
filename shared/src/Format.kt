sealed interface Piece {
    data class Text(
        val value: String,
    ) : Piece

    data class Link(
        val url: String,
        val label: String,
    ) : Piece

    object Break : Piece
}

private val tokenPattern = Regex("<(/?)([a-zA-Z0-9]+)([^>]*)>|([^<]+)")
private val hrefPattern = Regex("href\\s*=\\s*\"([^\"]*)\"")
private val whitespace = Regex("\\s+")

private fun decodeEntities(s: String): String =
    s
        .replace("&nbsp;", " ")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")

fun parseHtml(html: String): List<List<Piece>> {
    val paragraphs = mutableListOf<List<Piece>>()
    var current = mutableListOf<Piece>()
    var href: String? = null
    val label = StringBuilder()

    fun flush() {
        val cleaned = current.filterNot { it is Piece.Text && it.value.trim() == "|" }
        val hasContent =
            cleaned.any {
                it is Piece.Link || (it is Piece.Text && it.value.isNotBlank())
            }
        if (hasContent) paragraphs += cleaned
        current = mutableListOf()
    }

    for (m in tokenPattern.findAll(html)) {
        val rawText = m.groups[4]?.value
        if (rawText != null) {
            val text = decodeEntities(rawText).replace(whitespace, " ")
            if (href != null) label.append(text) else current += Piece.Text(text)
            continue
        }

        val closing = m.groupValues[1] == "/"
        when (m.groupValues[2].lowercase()) {
            "br" -> {
                current += Piece.Break
            }

            "p" -> {
                flush()
            }

            "a" -> {
                if (!closing) {
                    href = hrefPattern.find(m.groupValues[3])?.groupValues?.get(1)
                    label.clear()
                } else {
                    val url = href
                    if (url != null && !url.startsWith("mailto:")) {
                        current += Piece.Link(decodeEntities(url), label.toString().trim())
                    }
                    href = null
                }
            }
        }
    }
    flush()
    return paragraphs
}

fun StringBuilder.renderHtml(html: String) {
    parseHtml(html).forEachIndexed { paragraphIndex, paragraph ->
        if (paragraphIndex > 0) appendLine()

        paragraph.forEach { piece ->
            when (piece) {
                is Piece.Text -> {
                    append(piece.value)
                }

                is Piece.Link -> {
                    append(piece.label)
                    if (piece.url.isNotBlank() && piece.url != piece.label) {
                        append(" (").append(piece.url).append(')')
                    }
                }

                Piece.Break -> {
                    appendLine()
                }
            }
        }
    }
}
