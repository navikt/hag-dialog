package no.nav.helsearbeidsgiver

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import no.nav.helsearbeidsgiver.auth.AuthClient
import no.nav.helsearbeidsgiver.auth.dialogportenTokenGetter
import no.nav.helsearbeidsgiver.database.Database
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.helsesjekker.HelsesjekkService
import no.nav.helsearbeidsgiver.helsesjekker.naisRoutes
import no.nav.helsearbeidsgiver.kafka.configureKafkaConsumer
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import org.slf4j.LoggerFactory

fun main() {
    startServer()
}

fun startServer() {
    val logger = LoggerFactory.getLogger("App")

    logger.info("Setter opp database...")
    val database = Database()

    logger.info("Migrering starter...")
    database.migrate()
    logger.info("Migrering ferdig.")

    logger.info("Setter opp Unleash...")
    val unleashFeatureToggles = UnleashFeatureToggles()
    val authClient = AuthClient()

    logger.info("Setter opp DialogportenClient...")
    val dialogportenClient =
        DialogportenClient(
            baseUrl = Env.Altinn.baseUrl,
            ressurs = Env.Altinn.dialogportenRessurs,
            getToken = authClient.dialogportenTokenGetter(),
        )

    logger.info("Setter opp DialogRepository...")
    val dialogRepository = DialogRepository(database.db)

    logger.info("Starter server...")
    embeddedServer(
        factory = Netty,
        port = 8080,
        module = {
            routing {
                naisRoutes(HelsesjekkService(database.db))
            }
            configureKafkaConsumer(unleashFeatureToggles, dialogportenClient, dialogRepository)
        },
    ).start(wait = true)
}
