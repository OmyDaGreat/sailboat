private val tokenPattern = Regex("<(/?)([a-zA-Z0-9]+)([^>]*)>|([^<]+)")
private val hrefPattern = Regex("href\\s*=\\s*\"([^\"]*)\"")
private val whitespace = Regex("\\s+")
private val entityPattern = Regex("&(?:nbsp|quot|apos|lt|gt|amp|#39);")

private fun decodeEntities(s: String): String =
    entityPattern.replace(s) {
        when (it.value) {
            "&nbsp;" -> " "
            "&quot;" -> "\""
            "&apos;", "&#39;" -> "'"
            "&lt;" -> "<"
            "&gt;" -> ">"
            else -> "&"
        }
    }

fun StringBuilder.buildHtml(html: String) {
    val paragraph = StringBuilder()
    var paragraphHasContent = false
    var paragraphCount = 0
    var href: String? = null
    val label = StringBuilder()

    fun flush() {
        if (!paragraphHasContent) {
            paragraph.clear()
            return
        }
        if (paragraphCount++ > 0) appendLine()
        append(paragraph)
        paragraph.clear()
        paragraphHasContent = false
    }

    for (m in tokenPattern.findAll(html)) {
        val rawText = m.groups[4]?.value
        if (rawText != null) {
            val text = decodeEntities(rawText).replace(whitespace, " ")
            if (href != null) {
                label.append(text)
            } else if (text.trim() != "|") {
                paragraph.append(text)
                paragraphHasContent = paragraphHasContent || text.isNotBlank()
            }
            continue
        }

        val closing = m.groupValues[1] == "/"
        when (m.groupValues[2].lowercase()) {
            "br" -> {
                paragraph.appendLine()
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
                        val decodedUrl = decodeEntities(url)
                        val trimmedLabel = label.toString().trim()
                        paragraph.append(trimmedLabel)
                        if (decodedUrl.isNotBlank() && decodedUrl != trimmedLabel) {
                            paragraph.append(" (").append(decodedUrl).append(')')
                        }
                        paragraphHasContent = true
                    }
                    href = null
                }
            }
        }
    }
    flush()
}
