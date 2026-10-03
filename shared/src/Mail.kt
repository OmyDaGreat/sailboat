import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

suspend fun send(
    token: String,
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
            header("X-Proxy-Token", token)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString<MailRequest>(request))
        }
    if (response.status.value !in 200..299) {
        error("Mail request failed with HTTP ${response.status.value}")
    }
}
