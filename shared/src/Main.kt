import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.text.bold
import com.varabyte.kotter.foundation.text.cyan
import com.varabyte.kotter.foundation.text.link
import com.varabyte.kotter.foundation.text.p
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.text.underline
import com.varabyte.kotter.runtime.render.RenderScope
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

private val descriptionPattern =
    Regex("(<description(?:\\s[^>]*)?>)([\\s\\S]*?)(</description>)")

private val bareAmpersand =
    Regex("&(?!(?:amp|lt|gt|quot|apos|#\\d+|#x[0-9a-fA-F]+);)")

fun escapeXml(xml: String): String =
    descriptionPattern.replace(xml.replace(bareAmpersand, "&amp;")) { match ->
        val (opening, content, closing) = match.destructured
        opening + content.replace("<", "&lt;").replace(">", "&gt;") + closing
    }

fun declareNamespaces(xml: String): String =
    xml.replaceFirst(
        "<rss version=\"2.0\">",
        "<rss version=\"2.0\" " +
            "xmlns:geo=\"http://www.w3.org/2003/01/geo/wgs84_pos#\" " +
            "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" " +
            "xmlns:media=\"http://search.yahoo.com/mrss/\">",
    )

val d =
    "<rss version=\"2.0\">" +
        "<channel>\n" +
        "<title>Harvard SEAS Calendar</title>\n" +
        "<link>https://events.seas.harvard.edu/calendar</link>\n" +
        "<description>Harvard SEAS Calendar</description>\n" +
        "<lastBuildDate>Wed, 30 Sep 2026 15:44:56 -0400</lastBuildDate>\n" +
        "<ttl>120</ttl>\n" +
        "<language>en-us</language>\n" +
        "<generator>Localist</generator>\n" +
        "<item>\n" +
        "<title>\n" +
        "Sep 30, 2026: The White House Effect: Film Screening and Q&A with William Reilly at Vorenberg Classroom\n" +
        "</title>\n" +
        "<description>\n" +
        "<p>The White House Effect: How did climate change go from a scientific issue to a political crisis?</p> <p> </p> <p>Join Harvard Law School Professor Richard Lazarus for a special screening of The White House Effect and Q&amp;A with William Reilly, the EPA Administrator under President George H.W. Bush who played a major role in the events of the film. <br> </p> <p>Summary: Three decades ago, the world was poised to stop global warming. Using exclusively archival material, The White House Effect tells the dramatic origin story of the climate crisis and how a political battle in the George H.W. Bush administration changed the course of history. <br> </p> <p>We urge everyone attending to watch the trailer of the documentary prior to the event. It's a fantastic intro to the film, explains Mr Reilly’s pivotal role in the events covered, and can be seen on YouTube here <a href=\"https://www.youtube.com/watch?v=zNOxFwZLRZU\">https://www.youtube.com/watch?v=zNOxFwZLRZU</a></p> <p> </p> <p>Snacks will be served.</p> <p><a href=\"https://events.seas.harvard.edu/event/the-white-house-effect-film-screening-and-qa-with-william-reilly\">View on site</a> | <a href=\"mailto:?subject=I+found+an+interesting+event%3A+The+White+House+Effect%3A+Film+Screening+and+Q%26A+with+William+Reilly&amp;body=I+found+an+interesting+event+you+may+like%3A%0A%0A%0ADate%3A+Sep+30%2C+2026%0A%0ADescription%3A%0AThe+White+House+Effect%3A+How+did+climate+change+go+from+a+scientific+issue+to+a+political+crisis%3F%0A%0A+%0A%0AJoin+Harvard+Law+School+Professor+Richard+Lazarus+for+a+special+screening+of+The+White+House+Effect+and+Q%26A+with+William+Reilly%2C+the+EPA+Administrator+under+President+George+H.W.+Bush+who+played+a+major+role+in+the+events+of+the+film.%0A+%0A%0ASummary%3A+Three+decades+ago%2C+the+world+was+poised+to+stop+global+warming.+Using+exclusively+archival+material%2C+The+White+House+Effect+tells+the+dramatic+origin+story+of+the+climate+crisis+and+how+a+political+battle+in+the+George+H.W.+Bush+administration+changed+the+course+of+history.%0A+%0A%0AWe+urge+everyone+attending+to+watch+the+trailer+of+the+documentary+prior+to+the+event.+It%27s+a+fantastic+intro+to+the+film%2C+explains+Mr+Reilly%E2%80%99s+pivotal+role+in+the+events+covered%2C+and+can+be+seen+on+YouTube+here+https%3A%2F%2Fwww.youtube.com%2Fwatch%3Fv%3DzNOxFwZLRZU%0A%0A+%0A%0ASnacks+will+be+served.%0A%0Ahttps%3A%2F%2Fevents.seas.harvard.edu%2Fevent%2Fthe-white-house-effect-film-screening-and-qa-with-william-reilly%0A\">Email this event</a></p>\n" +
        "</description>\n" +
        "<guid isPermaLink=\"false\">tag:localist.com,2008:EventInstance_54062972954257</guid>\n" +
        "<geo:lat>42.378462</geo:lat>\n" +
        "<geo:long>-71.119228</geo:long>\n" +
        "<pubDate>Wed, 30 Sep 2026 16:00:00 -0400</pubDate>\n" +
        "<dc:date>2026-09-30T16:00:00-04:00</dc:date>\n" +
        "<link>\n" +
        "https://events.seas.harvard.edu/event/the-white-house-effect-film-screening-and-qa-with-william-reilly\n" +
        "</link>\n" +
        "<media:content medium=\"image\" url=\"https://localist-images.azureedge.net/photos/41775337349670/huge/699518e17901feab8cf7e731cd03e6f28e7ae79c.jpg\"/>\n" +
        "</item>\n" +
        "</channel>\n" +
        "</rss>"

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
    val rss = XML.v1 { policy { ignoreUnknownChildren() } }.decodeFromString<Rss>(escapeXml(declareNamespaces(d)))

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
