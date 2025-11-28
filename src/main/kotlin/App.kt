package no.nav.helsearbeidsgiver

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import no.nav.helsearbeidsgiver.auth.AuthClient
import no.nav.helsearbeidsgiver.auth.dialogportenTokenGetter
import no.nav.helsearbeidsgiver.database.Database
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.database.DokumentKoblingRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.dokumentKobling.SykmeldingJobb
import no.nav.helsearbeidsgiver.dokumentKobling.startRecurringJobs
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
    val dokumentKoblingRepository = DokumentKoblingRepository(database.db)
    val dialogportenService =
        DialogportenService(
            dialogRepository = dialogRepository,
            dialogportenClient = dialogportenClient,
            unleashFeatureToggles = unleashFeatureToggles,
        )

    val jobber =
        listOf(
            SykmeldingJobb(
                dokumentKoblingRepository = dokumentKoblingRepository,
                dialogportenService = dialogportenService,
            ),
        /*SykepengeSoeknadJobb(
            dokumentKoblingRepository = dokumentKoblingRepository,
            dialogportenService = dialogportenService
        )*/
        )

    logger.info("Starter server...")
    embeddedServer(
        factory = Netty,
        port = 8080,
        module = {
            routing {
                naisRoutes(HelsesjekkService(database.db))
            }
            configureKafkaConsumer(unleashFeatureToggles, dokumentKoblingRepository, dialogportenService)
            startRecurringJobs(jobber)
        },
    ).start(wait = true)
}
