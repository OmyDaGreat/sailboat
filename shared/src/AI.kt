import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking

fun getEventResults(
    token: String,
    topic: String,
    timeFromNow: String,
    items: List<Item>,
): String =
    fetchAIResponse(
        token,
        """
        Create the body of an email listing all upcoming events from the first event in the feed through $timeFromNow that relate to $topic.
        
        Return only the email body. Do not include a subject line, greeting, sign-off, commentary, Markdown, tables, HTML, or code fences.
        
        Use this exact format:
        
        Upcoming $topic events
        
        EVENT
        Title: <event title>
        Date and time: <date and time>
        Location: <location, or "Not specified">
        Organization: <hosting organization, or "Not specified">
        Calendar link: <full URL>
        Coordinates: <latitude>, <longitude> (only if useful; otherwise omit this line)
        
        Put multiple events if necessary using the same format (without the upcoming line, that only goes at the top) and separate events with one blank line. Use plain text only. Preserve full URLs so they remain usable as clickable links in plain-text email. Do not invent or infer missing information. If there are no matching events, return exactly:
        No upcoming $topic events were found.
        
        Here are the events:
        ...
        $${
            StringBuilder().apply {
                items.forEach { item ->
                    val title = item.title.trim().substringAfter(':', item.title.trim())

                    appendLine("${item.pubDate}:$title")
                    appendLine("Calendar link: ${item.link}")
                    appendLine("Latitude: ${item.latitude ?: "unknown"}")
                    appendLine("Longitude: ${item.longitude ?: "unknown"}")

                    buildHtml(item.description)
                    appendLine()
                }
            }
        }
        """.trimIndent(),
    )

private fun fetchAIResponse(
    token: String,
    prompt: String,
) = runBlocking {
    val request =
        AIRequest(
            model = "openai/gpt-4o-mini",
            messages = listOf(PromptMessage(role = "user", content = prompt)),
        )
    val response =
        http.post("https://ai.malefic.xyz/chat") {
            header("X-Proxy-Token", token)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString<AIRequest>(request))
        }

    if (response.status.value !in 200..299) {
        error("AI request failed with HTTP ${response.status.value}")
    }

    json
        .decodeFromString<AIResponse>(response.bodyAsText())
        .choices
        .firstOrNull()
        ?.message
        ?.content
        ?: error("The AI response did not contain anything")
}
