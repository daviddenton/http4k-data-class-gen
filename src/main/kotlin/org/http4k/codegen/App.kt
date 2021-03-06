package org.http4k.codegen

import GenerateFromJson
import GenerateFromXml
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
import org.http4k.filter.GsonGenerateXmlDataClasses
import org.http4k.filter.JacksonXmlGenerateXmlDataClasses
import org.http4k.format.Gson
import org.http4k.format.Gson.asPrettyJsonString
import org.http4k.format.Gson.json
import org.http4k.format.Xml.asXmlString
import org.http4k.format.Xml.xml
import org.http4k.lens.FormField
import org.http4k.lens.Header
import org.http4k.lens.Validator.Feedback
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
        val templates = if (System.getenv("DEVMODE") != null) HandlebarsTemplates().HotReload("src/main/resources") else HandlebarsTemplates().CachingClasspath()

        val app = routes(
            "/json" bind json(templates),
            "/xml" bind gsonXml(templates),
            "/jacksonXml" bind jacksonXml(templates),
            "/" bind GET to { Response(OK).body(templates(Index)) }
        )

        val setContentType = Filter { next ->
            {
                next(it).with(Header.CONTENT_TYPE of ContentType.TEXT_HTML)
            }
        }

        return setContentType.then(app)
    }

    private fun json(templates: TemplateRenderer): RoutingHttpHandler =
        routes(
            GET to { _: Request -> Response(OK).body(templates(GenerateFromJson())) },
            POST to { request: Request ->
                val inputField = FormField.json().required("input")

                val form = Body.webForm(Feedback, inputField).toLens()
                val formInstance = form(request)

                val response = if (formInstance.errors.isEmpty()) {
                    val input = inputField(formInstance)
                    ByteArrayOutputStream().use {
                        GenerateDataClasses(Gson, PrintStream(it)).then { Response(OK).body(input.asPrettyJsonString()) }(request)
                        val output = String(it.toByteArray(), UTF_8)
                        templates(GenerateFromJson(input.asPrettyJsonString(), output))
                    }
                } else {
                    templates(GenerateFromJson(formInstance.fields["input"]?.joinToString()
                        ?: "", error = "invalid input"))
                }
                Response(OK).body(response)
            }
        )

    private fun gsonXml(templates: TemplateRenderer): RoutingHttpHandler =
        routes(
            GET to { _: Request -> Response(OK).body(templates(GenerateFromXml())) },
            POST to { request: Request ->
                val inputField = FormField.xml().required("input")

                val form = Body.webForm(Feedback, inputField).toLens()
                val formInstance = form(request)

                val response = if (formInstance.errors.isEmpty()) {
                    val input = inputField(formInstance)

                    ByteArrayOutputStream().use {
                        GsonGenerateXmlDataClasses(PrintStream(it))
                            .then { Response(OK).body(input.asXmlString()) }(request)
                        val output = String(it.toByteArray(), UTF_8)
                        templates(GenerateFromXml(input.asXmlString(), output))
                    }
                } else {
                    templates(GenerateFromXml(formInstance.fields["input"]?.joinToString()
                        ?: "", error = "invalid input"))
                }
                Response(OK).body(response)
            }
        )

    private fun jacksonXml(templates: TemplateRenderer): RoutingHttpHandler =
        routes(
            GET to { _: Request -> Response(OK).body(templates(GenerateFromXml())) },
            POST to { request: Request ->
                val inputField = FormField.xml().required("input")

                val form = Body.webForm(Feedback, inputField).toLens()
                val formInstance = form(request)

                val response = if (formInstance.errors.isEmpty()) {
                    val input = inputField(formInstance)

                    ByteArrayOutputStream().use {
                        JacksonXmlGenerateXmlDataClasses(PrintStream(it))
                            .then { Response(OK).body(input.asXmlString()) }(request)
                        val output = String(it.toByteArray(), UTF_8)
                        templates(GenerateFromXml(input.asXmlString(), output))
                    }
                } else {
                    templates(GenerateFromXml(formInstance.fields["input"]?.joinToString()
                        ?: "", error = "invalid input"))
                }
                Response(OK).body(response)
            }
        )
}

fun main(args: Array<String>) {
    val port = if (args.isNotEmpty()) args[0].toInt() else 5000
    App().asServer(Jetty(port)).start()
}
