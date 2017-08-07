package org.http4k.codegen

import Index
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.GenerateDataClasses
import org.http4k.format.Jackson
import org.http4k.lens.FormField
import org.http4k.lens.FormValidator
import org.http4k.lens.Header
import org.http4k.lens.LensFailure
import org.http4k.lens.webForm
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.http4k.template.HandlebarsTemplates
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets

object App {
    operator fun invoke(): HttpHandler {
        val templates = HandlebarsTemplates().CachingClasspath()

        val inputField = FormField.optional("input")

        val form = Body.webForm(FormValidator.Strict, inputField).toLens()
        val app = routes(
            "/" to GET bind { _: Request -> Response(OK).body(templates(Index())) },
            "/" to POST bind { request: Request ->
                val formInstance = form(request)
                val baos = ByteArrayOutputStream()
                val input = inputField(formInstance)
                val response = try {
                    GenerateDataClasses(Jackson, PrintStream(baos)).then { Response(OK).body(input ?: "") }(request)
                    val output = String(baos.toByteArray(), StandardCharsets.UTF_8)
                    templates(Index(input ?: "", output))
                } catch (e: LensFailure) {
                    templates(Index(input ?: "", error = "invalid input ${e.message}"))
                } finally {
                    baos.close()
                }
                Response(OK).body(response)
            }
        )

        val setContentType = Filter { next ->
            {
                next(it).with(Header.Common.CONTENT_TYPE of ContentType.TEXT_HTML)
            }
        }

        return setContentType.then(app)
    }
}

fun main(args: Array<String>) {
    val port = if (args.isNotEmpty()) args[0].toInt() else 5000
    App().asServer(Jetty(port)).startAndBlock()
}
