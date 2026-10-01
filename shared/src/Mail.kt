import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

private const val MAIL_PROXY_TOKEN = "d881ea0aa2d2d879349b2249adc4d462119b75c2c0c587c47cadd351505226d6"

suspend fun send(
    to: String,
    subject: String = "your sailboat-sourced harvard seas events",
    body: String,
) {
    val request =
        MailRequest(
            to = to,
            subject = subject,
            body = body,
        )
    val response =
        http.post("https://mail.malefic.xyz/contact") {
            header("X-Proxy-Token", MAIL_PROXY_TOKEN)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString<MailRequest>(request))
        }
    if (response.status.value !in 200..299) {
        error("Mail request failed with HTTP ${response.status.value}")
    }
}
