package no.nav.helsearbeidsgiver.dialogporten

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.helsearbeidsgiver.database.DialogForPatch
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository
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
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

class SykepengerDialogportenService(
    private val dialogRepository: DialogRepository,
    private val dialogportenClient: DialogportenClient,
    unleashFeatureToggles: UnleashFeatureToggles,
    agNotifikasjonKlient: ArbeidsgiverNotifikasjonKlient,
    dokumentkoblingRepository: DokumentkoblingRepository,
) {
    private val logger = logger()
    private val sykmeldingHandler = SykmeldingHandler(dialogRepository, dialogportenClient, unleashFeatureToggles, agNotifikasjonKlient)
    private val sykepengesoeknadHandler =
        SykepengesoeknadHandler(
            dialogRepository,
            dialogportenClient,
            unleashFeatureToggles,
            agNotifikasjonKlient,
            dokumentkoblingRepository,
        )
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
        val foersteDag = LocalDate.of(2026, 3, 27)
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
                supervisorScope {
                    val semaphore = Semaphore(permits = 32) // max 32 corutines samtidig
                    dialoger.chunked(250).forEach { chunk ->
                        logger.info("Starter batch")
                        chunk
                            .map { dialogDto ->
                                async(Dispatchers.IO) {
                                    semaphore.withPermit {
                                        runCatching {
                                            patchEnkelDialogMedUrlFeil(dialogDto)
                                            opprettet.incrementAndGet()
                                        }.onFailure {
                                            sikkerLogger().error(
                                                "Feil ved patching av dialog ${dialogDto.dialogId}",
                                                it,
                                            )
                                            logger().error("Feil ved patching av dialog ${dialogDto.dialogId}")
                                            feilet.incrementAndGet()
                                        }
                                    }
                                }
                            }.awaitAll()
                        logger.info("Ferdig med batch")
                    }
                }
                totalOpprettet += opprettet.get()
                totalFeilet += feilet.get()
                logger.info("Ferdig med $dag - patchet OK: ${opprettet.get()}, feilet: ${feilet.get()}")
                logger.info("Ferdig med $dag - totalt OK: $totalOpprettet, totalt feilet: $totalFeilet")
            }
        logger.info("Ferdig med engangsjobb - totalt OK: $totalOpprettet, totalt feilet: $totalFeilet")
    }

    private suspend fun patchEnkelDialogMedUrlFeil(dialog: DialogForPatch) {
        val transmissions = dialog.transmissions
        transmissions
            .filter { it.dokumentType == LpsApiExtendedType.SYKMELDING.toString() }
            .also {
                if (it.isEmpty()) {
                    logger.warn(
                        "Ingen sykmelding transmission for dialog ${dialog.dialogId}",
                    )
                }
            }.forEach { transmission ->
                val sykmeldingTransmission = sykmeldingTransmission(transmission.dokumentId, isSilentUpdate = true)
                patchTransmission(
                    transmission = sykmeldingTransmission,
                    dialogId = dialog.dialogId,
                    transmissionId = transmission.transmissionId,
                )
            }

        transmissions
            .filter { it.dokumentType == LpsApiExtendedType.SYKEPENGESOEKNAD.toString() }
            .forEach { transmission ->
                val sykepengersoknadTransmission =
                    sykepengesoknadTransmission(
                        soeknadId = transmission.dokumentId,
                        isSilentUpdate = true,
                    )
                patchTransmission(
                    transmission = sykepengersoknadTransmission,
                    dialogId = dialog.dialogId,
                    transmissionId = transmission.transmissionId,
                )
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
            logger.error("Klarte ikke å fikse transmission $transmissionId i dialog $dialogId")
            sikkerLogger().error(
                "Klarte ikke å fikse transmission tittel: ${transmission.tittel}, $transmissionId i dialog $dialogId",
                e,
            )
            return false
        }
    }
}
