import org.http4k.template.ViewModel

object Index : ViewModel
data class Json(val input: String = "", val output: String? = null, val error: String? = null) : ViewModel
data class Xml(val input: String = "", val output: String? = null, val error: String? = null) : ViewModel