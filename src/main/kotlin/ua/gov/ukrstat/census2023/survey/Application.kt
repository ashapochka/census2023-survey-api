package ua.gov.ukrstat.census2023.survey

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import ua.gov.ukrstat.census2023.survey.plugins.configureMonitoring
import ua.gov.ukrstat.census2023.survey.plugins.configureRouting
import ua.gov.ukrstat.census2023.survey.plugins.configureSecurity
import ua.gov.ukrstat.census2023.survey.plugins.configureSerialization


fun main() {
    embeddedServer(
        Netty,
        port = 8080, host = "0.0.0.0",
        watchPaths = listOf("classes")
    ) {
        configureSecurity()
        configureMonitoring()
        configureSerialization()
        configureRouting()
    }.start(wait = true)
}
