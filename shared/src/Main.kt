import com.varabyte.kotter.foundation.input.input
import com.varabyte.kotter.foundation.input.onInputEntered
import com.varabyte.kotter.foundation.input.runUntilInputEntered
import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.serialization.XML

const val FEED_URL = "https://events.seas.harvard.edu/calendar.xml"

fun fetchFeed(url: String = FEED_URL): String =
    runBlocking {
        http.get(url).bodyAsText().replace("\r\n", "\n")
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

fun main() {
    session {
        var topic by liveVarOf("")
        var email by liveVarOf("")
        var timeFromNow by liveVarOf("")

        section {
            textLine("What topic would you like to search for?")
            text("> ")
            input()
        }.runUntilInputEntered {
            onInputEntered { topic = input.trim() }
        }

        section {
            textLine("What email address should the results be sent to?")
            text("> ")
            input()
        }.runUntilInputEntered {
            onInputEntered { email = input.trim() }
        }

        section {
            textLine("How far ahead should events be searched (e.g. 1 week, 2 months)?")
            text("> ")
            input()
        }.runUntilInputEntered {
            onInputEntered { timeFromNow = input.trim() }
        }

        val raw = fetchFeed()
        val rss = XML.v1 { policy { ignoreUnknownChildren() } }.decodeFromString<Rss>(escapeXml(declareNamespaces(raw)))

        section {
            textLine(getEventResults(topic, timeFromNow, rss.channel.items))
        }.run()
    }
}
