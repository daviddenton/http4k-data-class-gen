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
import org.http4k.format.Jackson.asPrettyJsonString
import org.http4k.format.Jackson.json
import org.http4k.lens.FormField
import org.http4k.lens.FormValidator
import org.http4k.lens.Header
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

        val inputField = FormField.json().required("input")

        val form = Body.webForm(FormValidator.Feedback, inputField).toLens()
        val app = routes(
            "/" to GET bind { _: Request -> Response(OK).body(templates(Index())) },
            "/" to POST bind { request: Request ->
                val formInstance = form(request)
                val baos = ByteArrayOutputStream()

                val response = if (formInstance.errors.isEmpty()) {
                    val input = inputField(formInstance)
                    try {
                        GenerateDataClasses(Jackson, PrintStream(baos)).then { Response(OK).body(input.asPrettyJsonString()) }(request)
                        val output = String(baos.toByteArray(), StandardCharsets.UTF_8)
                        templates(Index(input.asPrettyJsonString(), output))
                    } finally {
                        baos.close()
                    }
                } else {
                    templates(Index(formInstance.fields["input"]?.joinToString() ?: "", error = "invalid input"))
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
