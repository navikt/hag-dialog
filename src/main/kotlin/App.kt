package no.nav.helsearbeidsgiver

import dokumentkobling.DokumentkoblingService
import dokumentkobling.SykepengeSoeknadJobb
import dokumentkobling.SykmeldingJobb
import dokumentkobling.startRecurringJobs
import io.ktor.server.application.ApplicationStopPreparing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import no.nav.helsearbeidsgiver.auth.AuthClient
import no.nav.helsearbeidsgiver.auth.dialogportenTokenGetter
import no.nav.helsearbeidsgiver.database.Database
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.FritakDialogportenService
import no.nav.helsearbeidsgiver.dialogporten.SykepengerDialogportenService
import no.nav.helsearbeidsgiver.dokumentkobling.AvbrytForespoerselJobb
import no.nav.helsearbeidsgiver.dokumentkobling.AvbrytInntektsmeldingJobb
import no.nav.helsearbeidsgiver.dokumentkobling.AvbrytSykepengeSoeknadJobb
import no.nav.helsearbeidsgiver.dokumentkobling.AvbrytSykmeldingJobb
import no.nav.helsearbeidsgiver.dokumentkobling.ForespoerselJobb
import no.nav.helsearbeidsgiver.dokumentkobling.InntektsmeldingJobb
import no.nav.helsearbeidsgiver.helsesjekker.HelsesjekkService
import no.nav.helsearbeidsgiver.helsesjekker.naisRoutes
import no.nav.helsearbeidsgiver.kafka.configureKafkaConsumer
import no.nav.helsearbeidsgiver.metrikk.metrikkRoutes
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

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
    val sykePengerdialogportenClient =
        DialogportenClient(
            baseUrl = Env.Altinn.baseUrl,
            ressurs = Env.Altinn.sykepengerDialogportenRessurs,
            getToken = authClient.dialogportenTokenGetter(),
        )
    val fritakDialogportenClient =
        DialogportenClient(
            baseUrl = Env.Altinn.baseUrl,
            ressurs = Env.Altinn.fritakDialogportenRessurs,
            getToken = authClient.dialogportenTokenGetter(),
        )
    logger.info("Setter opp DialogRepository...")
    val dialogRepository = DialogRepository(database.db)
    val fritakDialogRepository =
        no.nav.helsearbeidsgiver.database
            .FritakDialogRepository(database.db)
    val dokumentkoblingRepository = DokumentkoblingRepository(db = database.db, maksAntallPerHenting = 5000)
    val dokumentKoblingService = DokumentkoblingService(dokumentkoblingRepository)
    val sykepengerDialogportenService =
        SykepengerDialogportenService(
            dialogRepository = dialogRepository,
            dialogportenClient = sykePengerdialogportenClient,
            unleashFeatureToggles = unleashFeatureToggles,
        )
    val fritakDialogportenService =
        FritakDialogportenService(
            fritakDialogRepository = fritakDialogRepository,
            dialogportenClient = fritakDialogportenClient,
        )
    val jobber =
        listOf(
            SykmeldingJobb(
                dokumentkoblingRepository = dokumentkoblingRepository,
                sykepengerDialogportenService = sykepengerDialogportenService,
                unleashFeatureToggles = unleashFeatureToggles,
            ),
            SykepengeSoeknadJobb(
                dokumentkoblingRepository = dokumentkoblingRepository,
                sykepengerDialogportenService = sykepengerDialogportenService,
                unleashFeatureToggles = unleashFeatureToggles,
            ),
            ForespoerselJobb(
                dokumentkoblingService = dokumentKoblingService,
                sykepengerDialogportenService = sykepengerDialogportenService,
                unleashFeatureToggles = unleashFeatureToggles,
            ),
            InntektsmeldingJobb(
                dokumentkoblingService = dokumentKoblingService,
                sykepengerDialogportenService = sykepengerDialogportenService,
                unleashFeatureToggles = unleashFeatureToggles,
            ),
            AvbrytSykmeldingJobb(
                dokumentkoblingRepository = dokumentkoblingRepository,
            ),
            AvbrytSykepengeSoeknadJobb(
                dokumentkoblingRepository = dokumentkoblingRepository,
            ),
            AvbrytForespoerselJobb(
                dokumentkoblingRepository = dokumentkoblingRepository,
            ),
            AvbrytInntektsmeldingJobb(
                dokumentkoblingRepository = dokumentkoblingRepository,
            ),
        )

    logger.info("Starter server...")
    embeddedServer(
        factory = Netty,
        port = 8080,
        module = {
            val startupExceptionHandler =
                CoroutineExceptionHandler { _, exception ->
                    logger.error("Feilet ved oppdatering av vedlegg for fritakskrav", exception)
                }

            launch(Dispatchers.Default + startupExceptionHandler) {
                fritakDialogportenService.replaceAttachmentsForKrav()
            }

            routing {
                naisRoutes(HelsesjekkService(database.db))
                metrikkRoutes()
            }
            // configureKafkaConsumer(unleashFeatureToggles, dokumentKoblingService, fritakDialogportenService)
            startRecurringJobs(jobber)
            monitor.subscribe(ApplicationStopPreparing) {
                logger.info("Applikasjonen stopper, avslutter eventuelle jobber...")
                jobber.forEach { it.stop() }
            }
        },
    ).start(wait = true)
}
