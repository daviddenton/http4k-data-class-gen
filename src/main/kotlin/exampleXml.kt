import org.http4k.core.Body
import org.http4k.format.Xml.auto

data class XmlExample(val root: XmlRoot)
data class XmlRoot(val child: List<XmlChild>)
data class XmlChild(val content: String)

fun main(args: Array<String>) {
    Body.auto<XmlExample>()

}