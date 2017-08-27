import org.http4k.template.ViewModel

object Index : ViewModel
data class GenerateFromJson(val input: String = "", val output: String? = null, val error: String? = null) : ViewModel
data class GenerateFromXml(val input: String = "", val output: String? = null, val error: String? = null) : ViewModel