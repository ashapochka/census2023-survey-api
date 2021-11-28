package ua.gov.ukrstat.census2023.survey

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import ua.gov.ukrstat.census2023.survey.plugins.configureMonitoring
import ua.gov.ukrstat.census2023.survey.plugins.configureRouting
import ua.gov.ukrstat.census2023.survey.plugins.configureSerialization
import ua.gov.ukrstat.census2023.survey.plugins.*


fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureRouting()
//        configureSecurity()
        configureMonitoring()
        configureSerialization()
    }.start(wait = true)
}
