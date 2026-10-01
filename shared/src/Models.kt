import io.ktor.client.HttpClient
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

val http = HttpClient()

val json = Json { ignoreUnknownKeys = true }

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
    @XmlSerialName(
        value = "lat",
        prefix = "geo",
        namespace = "http://www.w3.org/2003/01/geo/wgs84_pos#",
    )
    @XmlElement(true)
    val latitude: Double? = null,
    @XmlSerialName(
        value = "long",
        prefix = "geo",
        namespace = "http://www.w3.org/2003/01/geo/wgs84_pos#",
    )
    @XmlElement(true)
    val longitude: Double? = null,
)

@Serializable
data class MailRequest(
    val to: String,
    val subject: String,
    val body: String,
)

@Serializable
data class AIRequest(
    val model: String,
    val messages: List<PromptMessage>,
)

@Serializable
data class PromptMessage(
    val role: String,
    val content: String,
)

@Serializable
data class Message(
    val content: String,
)

@Serializable
data class Choices(
    val message: Message,
)

@Serializable
data class AIResponse(
    val choices: List<Choices>,
)
