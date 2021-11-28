package ua.gov.ukrstat.census2023.survey.plugins

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.lectra.koson.obj
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*


fun Application.configureRouting() {
    install(AutoHeadResponse)

    val mapper = jacksonObjectMapper()

    routing {
        authenticate("auth-basic") {
            get("/") {
                call.respond(mapOf("msg" to "Hail Census 2023 in UA!"))
            }

            get("/{respondent}") {
                val respId = call.parameters["respondent"]
                val answers = respId?.let { it1 -> DBAccess.selectRespondentLatestAnswer(it1) }
                call.respondText(
                    answers ?: obj {}.toString(),
                    ContentType.Application.Json
                )
            }

            post("/") {
                val requestBody: String = call.receiveText()
                log.debug(requestBody)

                try {
                    val answers = mapper.readValue<MutableMap<String, Any>>(requestBody)
                    val rowCount = DBAccess.insertRespondentAnswer(
                        answers["rid"] as String?, requestBody
                    )
                    if (rowCount == 1)
                        call.respond(mapOf("msg" to "answers inserted"))
                    else
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("msg" to "failed to insert answers")
                        )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.UnprocessableEntity,
                        mapOf("msg" to e.toString())
                    )
                }
            }
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

    private val insertRespondentAnswerQuery =
        """INSERT INTO answer (resp_id, payload)
            | VALUES ((?), (?));""".trimMargin()

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

    fun insertRespondentAnswer(respondentId: String?, answer: String): Int {
        dataSource.connection.use { c ->
            c.prepareStatement(insertRespondentAnswerQuery).use { st ->
                st.setString(1, respondentId)
                st.setString(2, answer)
                return st.executeUpdate()
            }
        }
    }
}
