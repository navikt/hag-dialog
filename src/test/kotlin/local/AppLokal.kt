package no.nav.helsearbeidsgiver

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
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
    val unleashFeatureToggles = UnleashFeatureToggles(Env.Application.local)

    logger.info("Setter opp DialogportenClient...")
    val dialogportenClient = mockk<DialogportenClient>(relaxed = true)

    coEvery { dialogportenClient.createDialog(any<CreateDialogRequest>()) } answers {
        println("createDialog called with: ${firstArg<CreateDialogRequest>()}")
        UUID.randomUUID()
    }
    coEvery { dialogportenClient.addTransmission(any(), any<Transmission>()) } answers {
        println("addTransmission called with dialogId: ${firstArg<UUID>()} and transmission: ${secondArg<Transmission>()}")
        UUID.randomUUID()
    }
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
