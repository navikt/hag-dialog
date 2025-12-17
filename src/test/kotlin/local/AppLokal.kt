package no.nav.helsearbeidsgiver

import dokumentkobling.SykepengeSoeknadJobb
import dokumentkobling.startRecurringJobs
import io.ktor.server.application.ApplicationStopPreparing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.helsearbeidsgiver.database.Database
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.dialogporten.domene.CreateDialogRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.Transmission
import no.nav.helsearbeidsgiver.helsesjekker.HelsesjekkService
import no.nav.helsearbeidsgiver.helsesjekker.naisRoutes
import no.nav.helsearbeidsgiver.kafka.configureKafkaConsumer
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import org.slf4j.LoggerFactory
import java.util.UUID

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

    logger.info("Setter opp DialogportenClient...")
    val dialogportenClient = mockk<DialogportenClient>(relaxed = true)

    coEvery { dialogportenClient.createDialog(any<CreateDialogRequest>()) } answers {
        logger.info("createDialog called with: ${firstArg<CreateDialogRequest>()}")
        UUID.randomUUID()
    }
    coEvery { dialogportenClient.addTransmission(any(), any<Transmission>()) } answers {
        logger.info("addTransmission called with dialogId: ${firstArg<UUID>()} and transmission: ${secondArg<Transmission>()}")
        UUID.randomUUID()
    }
    logger.info("Setter opp DialogRepository...")
    val dialogRepository = DialogRepository(database.db)
    val dokumentkoblingRepository = DokumentkoblingRepository(db = database.db, maksAntallPerHenting = 1000)

    val dialogportenService =
        DialogportenService(
            dialogRepository = dialogRepository,
            dialogportenClient = dialogportenClient,
            unleashFeatureToggles = unleashFeatureToggles,
        )

    val jobber =
        listOf(
            SykepengeSoeknadJobb(
                dokumentkoblingRepository = dokumentkoblingRepository,
                dialogportenService = dialogportenService,
                unleashFeatureToggles = unleashFeatureToggles,
            ),
        )

    logger.info("Starter server...")
    embeddedServer(
        factory = Netty,
        port = 8080,
        module = {
            routing {
                naisRoutes(HelsesjekkService(database.db))
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
