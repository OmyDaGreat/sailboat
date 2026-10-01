import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking

fun getEventResults(
    topic: String,
    timeFromNow: String,
    items: List<Item>,
): String =
    fetchAIResponse(
        """
        Create a concise list of all upcoming events that are happening from now to $timeFromNow and relate to $topic. Include the following information for each event:
        The latitude and longitude identify the event location. Do not invent a street address from coordinates. Include the coordinates only when they help clarify the location. You can use "https://maps.google.com/?q=<lat>,<long>" as a link for the coordinates as well.
        - Event title
        - Date and time
        - Location
        - Hosting organization
        - Link to the calendar listing

        Here's the list of all the events:

        ${
            StringBuilder().apply {
                items.forEach { item ->
                    val title = item.title.trim().substringAfter(':', item.title.trim())

                    appendLine("${item.pubDate}:$title")
                    appendLine("Calendar link: ${item.link}")
                    appendLine("Latitude: ${item.latitude ?: "unknown"}")
                    appendLine("Longitude: ${item.longitude ?: "unknown"}")

                    renderHtml(item.description)
                    appendLine()
                }
            }
        }
        """.trimIndent(),
    )

private fun fetchAIResponse(prompt: String) =
    runBlocking {
        val request =
            AIRequest(
                model = "anthropic/claude-sonnet-5.5",
                messages = listOf(PromptMessage(role = "user", content = prompt)),
            )
        val response =
            http.post("https://ai.malefic.xyz/chat") {
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
            ?: error("The AI response did not contain any choices")
    }
