package nl.myndocs.oauth2.ktor.feature.request

import io.ktor.application.ApplicationCall
import io.ktor.http.HttpStatusCode
import io.ktor.request.header
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.request.receiveParameters
import io.ktor.response.header
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.util.toMap
import kotlinx.coroutines.experimental.runBlocking
import nl.myndocs.oauth2.ktor.feature.json.JsonMapper
import nl.myndocs.oauth2.request.CallContext

class KtorCallContext(val applicationCall: ApplicationCall) : CallContext {
    override val path: String = applicationCall.request.path()
    override val method: String = applicationCall.request.httpMethod.value
    override val headers: Map<String, String> = applicationCall.request
            .headers
            .toMap()
            .mapValues { applicationCall.request.header(it.key) }
            .filterValues { it != null }
            .mapValues { it.value!! }

    override val queryParameters: Map<String, String> = applicationCall.request
            .queryParameters
            .toMap()
            .filterValues { it.isNotEmpty() }
            .mapValues { it.value.first() }

    private var _formParameters: Map<String, String>? = null
    override val formParameters: Map<String, String>
        get() = receiveParameters()

    private fun receiveParameters(): Map<String, String> {
        if (_formParameters == null) {
            _formParameters = runBlocking {
                applicationCall.receiveParameters()
                        .toMap()
                        .filterValues { it.isNotEmpty() }
                        .mapValues { it.value.first() }
            }
        }

        return _formParameters!!
    }

    override fun respondStatus(statusCode: Int) {
        applicationCall.response.status(HttpStatusCode.fromValue(statusCode))
    }

    override fun respondHeader(name: String, value: String) {
        applicationCall.response.header(name, value)
    }

    override fun respondJson(content: Any) {
        runBlocking {
            applicationCall.respondText(
                    JsonMapper.toJson(content),
                    io.ktor.http.ContentType.Application.Json
            )
        }
    }

    override fun redirect(uri: String) {
        runBlocking {
            applicationCall.respondRedirect(uri)
        }
    }
}