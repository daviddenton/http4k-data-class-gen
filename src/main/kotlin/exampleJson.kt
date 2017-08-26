import org.http4k.core.Body
import org.http4k.format.Jackson.auto

data class JsonExample(val root: JsonRoot)
data class JsonRoot(val child: List<JsonChild>)
data class JsonChild(val content: String)

fun main(args: Array<String>) {
    Body.auto<JsonExample>()
}