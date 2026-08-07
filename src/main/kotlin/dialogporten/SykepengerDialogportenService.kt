package no.nav.helsearbeidsgiver.dialogporten

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import no.nav.helsearbeidsgiver.database.DialogEntity
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.dialogporten.domene.TransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.toTransmission
import no.nav.helsearbeidsgiver.dialogporten.handlers.ForespoerselHandler
import no.nav.helsearbeidsgiver.dialogporten.handlers.InntektsmeldingHandler
import no.nav.helsearbeidsgiver.dialogporten.handlers.SykepengesoeknadHandler
import no.nav.helsearbeidsgiver.dialogporten.handlers.SykmeldingHandler
import no.nav.helsearbeidsgiver.dialogporten.handlers.UtgaattForespoerselHandler
import no.nav.helsearbeidsgiver.dialogporten.handlers.sykepengesoknadTransmission
import no.nav.helsearbeidsgiver.dialogporten.handlers.sykmeldingTransmission
import no.nav.helsearbeidsgiver.kafka.Inntektsmelding
import no.nav.helsearbeidsgiver.kafka.Inntektsmeldingsforespoersel
import no.nav.helsearbeidsgiver.kafka.Sykepengesoeknad
import no.nav.helsearbeidsgiver.kafka.Sykmelding
import no.nav.helsearbeidsgiver.kafka.UtgaattInntektsmeldingForespoersel
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

class SykepengerDialogportenService(
    private val dialogRepository: DialogRepository,
    private val dialogportenClient: DialogportenClient,
    unleashFeatureToggles: UnleashFeatureToggles,
) {
    private val logger = logger()
    private val sykmeldingHandler = SykmeldingHandler(dialogRepository, dialogportenClient, unleashFeatureToggles)
    private val sykepengesoeknadHandler = SykepengesoeknadHandler(dialogRepository, dialogportenClient)
    private val forespoerselHandler = ForespoerselHandler(dialogRepository, dialogportenClient)
    private val inntektsmeldingHandler = InntektsmeldingHandler(dialogRepository, dialogportenClient)
    private val utgaattForespoerselHandler = UtgaattForespoerselHandler(dialogRepository, dialogportenClient)

    fun opprettOgLagreDialog(sykmelding: Sykmelding) {
        sykmeldingHandler.opprettOgLagreDialog(sykmelding)
    }

    fun oppdaterDialogMedSykepengesoeknad(sykepengesoeknad: Sykepengesoeknad) {
        sykepengesoeknadHandler.oppdaterDialog(sykepengesoeknad)
    }

    fun oppdaterDialogMedInntektsmeldingsforespoersel(inntektsmeldingsforespoersel: Inntektsmeldingsforespoersel) {
        oppdaterDialogMedInntektsmeldingsforespoersel(
            forespoerselId = inntektsmeldingsforespoersel.forespoerselId,
            sykmeldingId = inntektsmeldingsforespoersel.sykmeldingId,
        )
    }

    fun oppdaterDialogMedInntektsmeldingsforespoersel(
        forespoerselId: UUID,
        sykmeldingId: UUID,
    ) {
        forespoerselHandler.oppdaterDialog(forespoerselId = forespoerselId, sykmeldingId = sykmeldingId)
    }

    fun oppdaterDialogMedUtgaattForespoersel(utgaattForespoersel: UtgaattInntektsmeldingForespoersel) {
        oppdaterDialogMedUtgaattForespoersel(
            forespoerselId = utgaattForespoersel.forespoerselId,
            sykmeldingId = utgaattForespoersel.sykmeldingId,
        )
    }

    fun oppdaterDialogMedUtgaattForespoersel(
        forespoerselId: UUID,
        sykmeldingId: UUID,
    ) {
        utgaattForespoerselHandler.oppdaterDialog(forespoerselId = forespoerselId, sykmeldingId = sykmeldingId)
    }

    fun oppdaterDialogMedInntektsmelding(inntektsmelding: Inntektsmelding) {
        inntektsmeldingHandler.oppdaterDialog(inntektsmelding)
    }

    suspend fun oppdaterTransmisjonerMedFeilUrl() {
        val foersteDag = LocalDate.of(2026, 1, 5)
        val sisteDagInklusiv = LocalDate.of(2026, 5, 28)
        var totalOpprettet = 0
        var totalFeilet = 0

        logger.info("Starter å fikse transmission-URLer fra og med $foersteDag til og med $sisteDagInklusiv")

        generateSequence(foersteDag) { it.plusDays(1) }
            .takeWhile { !it.isAfter(sisteDagInklusiv) }
            .forEach { dag ->
                logger.info("Starter prosessering av transmission url fix for $dag")
                val dialoger = dialogRepository.hentDialogerOpprettetPaaDag(dag)
                logger.info("Fant ${dialoger.size} dialoger opprettet som skal fikses for $dag")
                val opprettet = AtomicInteger(0)
                val feilet = AtomicInteger(0)

                val semaphore = Semaphore(permits = 64) // max 64 corutines samtidig
                coroutineScope {
                    dialoger.forEach { dialog ->
                        launch {
                            semaphore.withPermit {
                                // enkel måte å forsikre vi gjør max 64 dialogporten kall i sekundet
                                delay(1000.milliseconds)
                                patchEnkelDialogMedUrlFeil(dialog, dag, opprettet, feilet)
                            }
                        }
                    }
                }

                totalFeilet += feilet.get()
                totalOpprettet += opprettet.get()
                logger.info(
                    "Ferdig med å fikse transmission-url for $dag. Oppdatert ${opprettet.get()}," +
                        "feilet ${feilet.get()} | Total oppdatert: $totalOpprettet, Total feilet $totalFeilet",
                )
            }
        logger.info("Ferdig med engangsjobb. Oppdatert $totalOpprettet, feilet $totalFeilet.")
    }

    private suspend fun patchEnkelDialogMedUrlFeil(
        dialog: DialogEntity,
        dag: LocalDate,
        opprettet: AtomicInteger,
        feilet: AtomicInteger,
    ) {
        val transmissions =
            transaction {
                dialog.transmissions.toList()
            }

        transmissions
            .filter { it.dokumentType == LpsApiExtendedType.SYKMELDING.toString() }
            .also {
                if (it.isEmpty()) {
                    logger.warn(
                        "Ingen sykmelding transmission for dialog ${dialog.dialogId} på $dag",
                    )
                }
            }.forEach { transmission ->
                val sykmeldingTransmission = sykmeldingTransmission(transmission.dokumentId, isSilentUpdate = true)
                patchTransmission(
                    transmission = sykmeldingTransmission,
                    dialogId = dialog.dialogId,
                    transmissionId = transmission.id.value,
                ).also { success ->
                    if (success) opprettet.incrementAndGet() else feilet.incrementAndGet()
                }
            }

        transmissions
            .filter { it.dokumentType == LpsApiExtendedType.SYKEPENGESOEKNAD.toString() }
            .forEach { transmission ->
                val sykepengersoknadTransmission = sykepengesoknadTransmission(transmission.dokumentId, isSilentUpdate = true)
                patchTransmission(
                    transmission = sykepengersoknadTransmission,
                    dialogId = dialog.dialogId,
                    transmissionId = transmission.id.value,
                ).also { success ->
                    if (success) opprettet.incrementAndGet() else feilet.incrementAndGet()
                }
            }
    }

    suspend fun patchTransmission(
        transmission: TransmissionRequest,
        dialogId: UUID,
        transmissionId: UUID,
    ): Boolean {
        try {
            dialogportenClient.replaceTransmission(
                dialogId,
                transmissionId,
                transmission.toTransmission(),
            )
            return true
        } catch (e: Exception) {
            logger.error("Klarte ikke å fikse søknad-transmission $transmissionId i dialog $dialogId")
            sikkerLogger().error("Klarte ikke å fikse søknad-transmission $transmissionId i dialog $dialogId", e)
            return false
        }
    }
}
