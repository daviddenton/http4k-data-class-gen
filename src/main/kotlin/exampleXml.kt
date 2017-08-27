package xml

import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.format.Xml.auto

// this XML...
val xml = """<XmlRoot>
                <XmlChild number="1">hello</XmlChild>
                <XmlChild number="2">there</XmlChild>
             </XmlRoot>
             """

// results in these data classes...
data class XmlBase(val XmlRoot: XmlRoot?)

data class XmlChild110601346(val number: Number?, val content: String?)

data class XmlRoot(val XmlChild: List<XmlChild110601346>?)


// use the lens like this
fun main(args: Array<String>) {
    val lens = Body.auto<XmlBase>().toLens()

    val request = Request(Method.GET, "/somePath").body(xml)

    val extracted: XmlBase = lens.extract(request)

    println(extracted)

}