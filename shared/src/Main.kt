import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.text.bold
import com.varabyte.kotter.foundation.text.cyan
import com.varabyte.kotter.foundation.text.link
import com.varabyte.kotter.foundation.text.p
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.text.underline
import com.varabyte.kotter.runtime.render.RenderScope
import io.ktor.client.HttpClient
import io.ktor.client.engine.curl.Curl
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("rss")
data class Rss(
    val channel: Channel,
)

@Serializable
@XmlSerialName("channel")
data class Channel(
    @XmlElement(true) val title: String,
    @XmlSerialName("item")
    val items: List<Item>,
)

@Serializable
data class Item(
    @XmlElement(true) val title: String,
    @XmlElement(true) val description: String,
    @XmlElement(true) val pubDate: String,
    @XmlElement(true) val link: String,
)

const val FEED_URL = "https://events.seas.harvard.edu/calendar.xml"

fun fetchFeed(url: String = FEED_URL): String =
    runBlocking {
        HttpClient(Curl).use { client ->
            client.get(url).bodyAsText().replace("\r\n", "\n")
        }
    }

private val descriptionPattern = Regex("(<description(?:\\s[^>]*)?>)([\\s\\S]*?)(</description>)")

private val bareAmpersand = Regex("&(?!(?:amp|lt|gt|quot|apos|#\\d+|#x[0-9a-fA-F]+);)")

fun escapeXml(xml: String): String =
    descriptionPattern
        .replace(xml) { match ->
            val (opening, content, closing) = match.destructured
            val trimmed = content.trim()
            val escaped =
                if (trimmed.startsWith("<![CDATA[") && trimmed.endsWith("]]>")) {
                    trimmed
                        .removePrefix("<![CDATA[")
                        .removeSuffix("]]>")
                        .replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                } else {
                    content
                        .replace(bareAmpersand, "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                }
            opening + escaped + closing
        }.replace(bareAmpersand, "&amp;")

fun declareNamespaces(xml: String): String =
    xml.replaceFirst(
        "<rss version=\"2.0\">",
        "<rss version=\"2.0\" " +
            "xmlns:geo=\"http://www.w3.org/2003/01/geo/wgs84_pos#\" " +
            "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" " +
            "xmlns:media=\"http://search.yahoo.com/mrss/\">",
    )

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

fun RenderScope.renderHtml(html: String) {
    for (paragraph in parseHtml(html)) {
        p {
            var atLineStart = true
            paragraph.forEach { piece ->
                when (piece) {
                    is Piece.Text -> {
                        val t = if (atLineStart) piece.value.trimStart() else piece.value
                        if (t.isNotEmpty()) {
                            text(t)
                            atLineStart = false
                        }
                    }

                    is Piece.Link -> {
                        underline { cyan { link(piece.url, piece.label) } }
                        atLineStart = false
                    }

                    Piece.Break -> {
                        textLine()
                        atLineStart = true
                    }
                }
            }
        }
    }
}

fun main() {
    val raw = fetchFeed()
    val rss = XML.v1 { policy { ignoreUnknownChildren() } }.decodeFromString<Rss>(escapeXml(declareNamespaces(raw)))

    session {
        section {
            bold { textLine(rss.channel.title) }
            bold { textLine("${rss.channel.items.size} event(s)") }
            textLine()
        }.run()
        rss.channel.items.forEach { item ->
            section {
                bold { textLine(item.title.trim()) }
                renderHtml(item.description)
                textLine()
            }.run()
        }
    }
}
