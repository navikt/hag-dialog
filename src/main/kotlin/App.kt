package no.nav.helsearbeidsgiver

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.helsearbeidsgiver.auth.AuthClient
import no.nav.helsearbeidsgiver.auth.dialogportenTokenGetter
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.kafka.MeldingTolker
import no.nav.helsearbeidsgiver.kafka.startKafkaConsumer
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import org.slf4j.LoggerFactory

fun main() {
    startServer()
}

fun startServer() {
    val logger = LoggerFactory.getLogger("App")

    logger.info("Setter opp Unleash...")
    val unleashFeatureToggles = UnleashFeatureToggles()
    val authClient = AuthClient()

    logger.info("Setter opp DialogportenService...")
    val dialogportenClient =
        DialogportenClient(
            baseUrl = Env.altinnBaseUrl,
            ressurs = Env.altinnImRessurs,
            getToken = authClient.dialogportenTokenGetter(),
        )

    logger.info("Starter server...")
    embeddedServer(
        factory = Netty,
        port = 8080,
        module = {
            startKafkaConsumer(
                meldingTolker =
                    MeldingTolker(
                        unleashFeatureToggles = unleashFeatureToggles,
                        dialogportenService = DialogportenService(dialogportenClient),
                    ),
            )
        },
    ).start(wait = true)
}
