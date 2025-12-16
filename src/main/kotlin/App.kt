package no.nav.helsearbeidsgiver

import dokumentkobling.DokumentkoblingService
import dokumentkobling.SykepengeSoeknadJobb
import dokumentkobling.SykmeldingJobb
import dokumentkobling.startRecurringJobs
import io.ktor.server.application.ApplicationStopPreparing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import no.nav.helsearbeidsgiver.auth.AuthClient
import no.nav.helsearbeidsgiver.auth.dialogportenTokenGetter
import no.nav.helsearbeidsgiver.database.Database
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.dokumentkobling.ForespoerselJobb
import no.nav.helsearbeidsgiver.dokumentkobling.InntektsmeldingJobb
import no.nav.helsearbeidsgiver.helsesjekker.HelsesjekkService
import no.nav.helsearbeidsgiver.helsesjekker.naisRoutes
import no.nav.helsearbeidsgiver.kafka.configureKafkaConsumer
import no.nav.helsearbeidsgiver.metrikk.metrikkRoutes
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
    val dokumentkoblingRepository = DokumentkoblingRepository(db = database.db, maksAntallPerHenting = 1000)
    val dokumentKoblingService = DokumentkoblingService(dokumentkoblingRepository)
    val dialogportenService =
        DialogportenService(
            dialogRepository = dialogRepository,
            dialogportenClient = dialogportenClient,
            unleashFeatureToggles = unleashFeatureToggles,
        )

    val jobber =
        listOf(
            SykmeldingJobb(
                dokumentkoblingRepository = dokumentkoblingRepository,
                dialogportenService = dialogportenService,
            ),
            SykepengeSoeknadJobb(
                dokumentkoblingRepository = dokumentkoblingRepository,
                dialogportenService = dialogportenService,
            ),
            ForespoerselJobb(
                dokumentkoblingService = dokumentKoblingService,
                dialogportenService = dialogportenService,
            ),
            InntektsmeldingJobb(
                dokumentkoblingService = dokumentKoblingService,
                dialogportenService = dialogportenService,
            ),
        )

    logger.info("Starter server...")
    embeddedServer(
        factory = Netty,
        port = 8080,
        module = {
            routing {
                naisRoutes(HelsesjekkService(database.db))
                metrikkRoutes()
            }
            configureKafkaConsumer(unleashFeatureToggles, dokumentkoblingRepository)
            startRecurringJobs(jobber)
            monitor.subscribe(ApplicationStopPreparing) {
                logger.info("Applikasjonen stopper, avslutter eventuelle jobber...")
                jobber.forEach { it.stop() }
            }
        },
    ).start(wait = true)
}
