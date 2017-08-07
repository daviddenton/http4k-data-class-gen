import org.http4k.template.ViewModel

data class Index(val input: String = "", val output: String? = null, val error: String? = null) : ViewModel