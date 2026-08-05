package no.nav.helsearbeidsgiver.dialogporten

import kotlinx.coroutines.delay
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.dialogporten.domene.Transmission
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
import org.jetbrains.exposed.exceptions.ExposedSQLException
import java.time.LocalDate
import java.util.UUID
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

    // Engangsjobb: fikser sykmelding- og sykepengesøknad-transmissions som fikk feil (dev-) url i prod.
    suspend fun oppdaterTransmisjonerMedFeilUrl() {
        val foersteDag = LocalDate.of(2026, 1, 5)
        val sisteDag = LocalDate.of(2026, 5, 28)
        var antallOppdatert = 0
        var antallFeilet = 0

        logger.info("Starter å fikse transmission-urler for dialoger opprettet fra og med $foersteDag til og med ${sisteDag.minusDays(1)}")

        generateSequence(foersteDag) { it.plusDays(1) }
            .takeWhile { it.isBefore(sisteDag) }
            .forEach { dag ->
                logger.info("Starter prossesering av transmission url fix for $dag")
                val dialoger = dialogRepository.hentDialogerOpprettetPaaDag(dag)
                logger.info("Fant ${dialoger.size} dialoger opprettet som skal fikses for $dag")
                val oppretterStart = antallOppdatert
                val feiletStart = antallFeilet

                dialoger.forEach { dialog ->

                    if (dialog.transmissions.empty()) {
                        logger.warn("Fant ingen transmissions for dialog ${dialog.dialogId} opprettet $dag")
                        return@forEach
                    }

                    dialog
                        .transmissionsByDokumentType(LpsApiExtendedType.SYKMELDING.toString())
                        .also { if (it.isEmpty()) logger.warn("Ingen sykmelding transmission for dialog ${dialog.dialogId} på $dag") }
                        .forEach { transmission ->
                            patchTransmission(
                                transmission = sykmeldingTransmission(transmission.dokumentId, isSilentUpdate = true),
                                dialogId = dialog.dialogId,
                                transmissionId = transmission.id.value,
                            ).also { success ->
                                if (success) antallOppdatert++ else antallFeilet++
                            }
                        }

                    dialog
                        .transmissionsByDokumentType(LpsApiExtendedType.SYKEPENGESOEKNAD.toString())
                        .forEach { transmission ->
                            patchTransmission(
                                transmission = sykepengesoknadTransmission(transmission.dokumentId, isSilentUpdate = true),
                                dialogId = dialog.dialogId,
                                transmissionId = transmission.id.value,
                            ).also { success ->
                                if (success) antallOppdatert++ else antallFeilet++
                            }
                        }
                }

                logger.info(
                    "Ferdig med fikse transmission-url for $dag. Oppdatert ${antallOppdatert - oppretterStart}," +
                        "feilet ${antallFeilet - feiletStart} | Total oppdatert: $antallOppdatert, Total feilet $antallFeilet",
                )
            }

        logger.info("Ferdig med å fikse transmission-urler. Oppdatert $antallOppdatert, feilet $antallFeilet.")
    }

    suspend fun patchTransmission(
        transmission: TransmissionRequest,
        dialogId: UUID,
        transmissionId: UUID,
    ): Boolean {
        transmission.attachments.firstOrNull()?.urls?.firstOrNull()?.let { url ->
            // TODO: Testing i dev slett, skal slettes
            logger.info("Patching ${transmission.id} to $dialogId with url $url")
        }
        try {
            dialogportenClient.replaceTransmission(
                dialogId,
                transmissionId,
                transmission.toTransmission(),
            )
            return true
        } catch (e: Exception) {
            sikkerLogger().error("Klarte ikke å fikse søknad-transmission $transmissionId i dialog $dialogId", e)
            return false
        }
    }
}
