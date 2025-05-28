package no.nav.helsearbeidsgiver.auth

import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.parameters
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.utils.createHttpClient
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

class AuthClient {
    private val sikkerLogger = sikkerLogger()
    private val httpClient = createHttpClient()

    fun tokenGetter(target: String): () -> String =
        {
            runBlocking {
                token(target).accessToken
            }
        }

    private suspend fun token(target: String): TokenResponse =
        try {
            httpClient
                .submitForm(
                    url = Env.Nav.naisTokenEndpoint,
                    formParameters =
                        parameters {
                            append("identity_provider", "maskinporten")
                            append("target", target)
                        },
                ).body()
        } catch (e: ResponseException) {
            e.logAndRethrow()
        }

    suspend fun altinnExchange(token: String): String =
        httpClient
            .get(Env.Altinn.tokenAltinn3ExchangeEndpoint) {
                bearerAuth(token)
            }.bodyAsText()
            .replace("\"", "")

    private suspend fun ResponseException.logAndRethrow(): Nothing {
        val error = response.body<ErrorResponse>()
        val msg = "Klarte ikke hente token. Feilet med status '${response.status}' og feilmelding '${error.errorDescription}'."

        sikkerLogger.error(msg)
        throw this
    }
}

private fun AuthClient.hentAltinnToken(target: String): () -> String {
    val maskinportenTokenGetter = tokenGetter(target = target)

    return {
        runBlocking {
            altinnExchange(maskinportenTokenGetter())
        }
    }
}

fun AuthClient.dialogportenTokenGetter() = hentAltinnToken(Env.Altinn.dialogportenScope)
