import kotlinx.serialization.Serializable
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

@Serializable
data class Message(
    val content: String,
)

@Serializable
data class Choices(
    val message: Message,
)

@Serializable
data class AI(
    val choices: List<Choices>,
)
