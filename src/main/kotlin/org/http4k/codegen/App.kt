package org.http4k.codegen

import Index
import Json
import Xml
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
import org.http4k.format.JacksonXml
import org.http4k.format.JacksonXml.asXmlString
import org.http4k.format.JacksonXml.xml
import org.http4k.lens.FormField
import org.http4k.lens.FormValidator
import org.http4k.lens.Header
import org.http4k.lens.webForm
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.http4k.template.HandlebarsTemplates
import org.http4k.template.TemplateRenderer
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets.UTF_8

object App {
    operator fun invoke(): HttpHandler {
        val templates = HandlebarsTemplates().CachingClasspath()

        val app = routes(
            "/json" bind json(templates),
            "/xml" bind xml(templates),
            "/" bind GET to { Response(OK).body(templates(Index)) }
        )

        val setContentType = Filter { next ->
            {
                next(it).with(Header.Common.CONTENT_TYPE of ContentType.TEXT_HTML)
            }
        }

        return setContentType.then(app)
    }

    private fun json(templates: TemplateRenderer): RoutingHttpHandler =
        routes(
            GET to { _: Request -> Response(OK).body(templates(Json())) },
            POST to { request: Request ->
                val inputField = FormField.json().required("input")

                val form = Body.webForm(FormValidator.Feedback, inputField).toLens()
                val formInstance = form(request)

                val response = if (formInstance.errors.isEmpty()) {
                    val input = inputField(formInstance)
                    ByteArrayOutputStream().use {
                        GenerateDataClasses(Jackson, PrintStream(it)).then { Response(OK).body(input.asPrettyJsonString()) }(request)
                        val output = String(it.toByteArray(), UTF_8)
                        templates(Json(input.asPrettyJsonString(), output))
                    }
                } else {
                    templates(Json(formInstance.fields["input"]?.joinToString() ?: "", error = "invalid input"))
                }
                Response(OK).body(response)
            }
        )

    private fun xml(templates: TemplateRenderer): RoutingHttpHandler =
        routes(
            GET to { _: Request -> Response(OK).body(templates(Xml())) },
            POST to { request: Request ->
                val inputField = FormField.xml().required("input")

                val form = Body.webForm(FormValidator.Feedback, inputField).toLens()
                val formInstance = form(request)

                val response = if (formInstance.errors.isEmpty()) {
                    val input = inputField(formInstance)

                    val convertToJson = Filter { next ->
                        {
                            val newBody = Jackson.compact(JacksonXml.mapper.readTree(input.asXmlString()))
                            next(it.body(newBody.replace("\"\":", "\"text\":")))
                        }
                    }

                    ByteArrayOutputStream().use {
                        convertToJson.then(GenerateDataClasses(Jackson, PrintStream(it)))
                            .then { Response(OK).body(it.bodyString()) }(request)
                        val output = String(it.toByteArray(), UTF_8)
                        templates(Xml(input.asXmlString(), output))
                    }
                } else {
                    templates(Xml(formInstance.fields["input"]?.joinToString() ?: "", error = "invalid input"))
                }
                Response(OK).body(response)
            }
        )
}

fun main(args: Array<String>) {
    val port = if (args.isNotEmpty()) args[0].toInt() else 5000
    App().asServer(Jetty(port)).startAndBlock()
}
