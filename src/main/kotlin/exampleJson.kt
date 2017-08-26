
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.format.Jackson.auto

// this JSON...
val json = """{"jsonRoot":{"child":["hello","there"],"number":123}}"""

// results in this...
data class Base(val jsonRoot: JsonRoot?)
data class JsonRoot(val child: List<String>?, val number: Number?)

// use the lens like this
fun main(args: Array<String>) {
    val lens = Body.auto<Base>().toLens()

    val request = Request(GET, "/somePath").body(json)

    val e: Base = lens.extract(request)

    println(e)

    println(lens.inject(e, Request(GET, "/somePath")).bodyString())
}