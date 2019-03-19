package json

import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.format.Jackson.auto

// this JSON...
val json = """{"jsonRoot":{"child":["hello","there"],"num":123}}"""

// results in these data classes...
data class Base(val jsonRoot: JsonRoot?)
data class JsonRoot(val child: List<String>?,
                    val num: Number?)

// use the lens like this
fun main() {
    val lens = Body.auto<Base>().toLens()

    val request = Request(GET, "/somePath").body(json)

    val extracted: Base = lens.extract(request)

    println(extracted)

    val injected = lens.inject(extracted, Request(GET, "/somePath"))

    println(injected.bodyString())
}