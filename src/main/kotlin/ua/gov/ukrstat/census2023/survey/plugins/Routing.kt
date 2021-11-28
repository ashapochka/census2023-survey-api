package ua.gov.ukrstat.census2023.survey.plugins

import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.routing.*


fun Application.configureRouting() {
    install(AutoHeadResponse)


    routing {
        get("/") {
            call.respondText("{\"msg\": \"Hail Census 2023!\"}", ContentType.Application.Json)
        }
        get("/{respondent}") {
            val respId = call.parameters["respondent"]
            val answers = respId?.let { it1 -> DBAccess.selectRespondentLatestAnswer(it1) }
            call.respondText(answers ?: "{}", ContentType.Application.Json)
        }
        // Static plugin. Try to access `/static/index.html`
        static("/static") {
            resources("static")
        }
    }
}

object DBAccess {
    private val dataSource: HikariDataSource = HikariDataSource()

    init {
        dataSource.jdbcUrl = System.getenv("census2023_survey_jdbc_url")
        dataSource.username = System.getenv("census2023_survey_jdbc_user")
        dataSource.password = System.getenv("census2023_survey_jdbc_password")
    }

    private val selectRespondentLatestAnswerQuery =
        """SELECT * FROM answer
            | WHERE resp_id = (?) 
            | ORDER BY mod_time DESC LIMIT 1;""".trimMargin()

    fun selectRespondentLatestAnswer(respondentId: String): String? {
        dataSource.connection.use { c ->
            c.prepareStatement(selectRespondentLatestAnswerQuery).use { st ->
                st.setString(1, respondentId)
                st.executeQuery().use { rs ->
                    var payload: String? = null
                    if (rs.next())
                        payload = rs.getString("payload")
                    return payload
                }
            }
        }
    }
}
