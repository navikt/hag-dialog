package no.nav.helsearbeidsgiver.dialogporten

import no.nav.helsearbeidsgiver.database.DialogRepository
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
import java.time.LocalDate
import java.util.UUID

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

    suspend fun testLesingAvTransmissionId() {
        val foersteDag = LocalDate.of(2026, 4, 28)
        val sisteDag = LocalDate.of(2026, 5, 28)

        generateSequence(foersteDag) { it.plusDays(1) }
            .takeWhile { it.isBefore(sisteDag) }
            .forEach { dag ->
                val dialoger = dialogRepository.hentDialogerOpprettetPaaDag(dag)
                val id =
                    dialoger
                        .first()
                        .transmissions
                        .first()
                        .id
                logger.info("Første transmission ID for dialog opprettet $dag: $id")
            }
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
                val dialoger = dialogRepository.hentDialogerOpprettetPaaDag(dag)
                logger.info("Fant ${dialoger.size} dialoger opprettet $dag")

                dialoger.forEach { dialog ->
                    // Sykmelding: dialogen har alltid nøyaktig én
                    dialog
                        .transmissionsByDokumentType(LpsApiExtendedType.SYKMELDING.toString())
                        .forEach { transmisjon ->
                            try {
                                dialogportenClient.replaceTransmission(
                                    dialog.dialogId,
                                    transmisjon.id.value,
                                    sykmeldingTransmission(transmisjon.dokumentId, isSilentUpdate = true).toTransmission(),
                                )
                                antallOppdatert++
                            } catch (e: Exception) {
                                antallFeilet++
                                logger.error(
                                    "Klarte ikke å fikse sykmelding-transmission ${transmisjon.id.value} " +
                                        "i dialog ${dialog.dialogId} for dokumentId ${transmisjon.dokumentId}",
                                    e,
                                )
                            }
                        }

                    // Sykepengesøknad: dialogen kan mangle denne eller ha flere
                    dialog
                        .transmissionsByDokumentType(LpsApiExtendedType.SYKEPENGESOEKNAD.toString())
                        .forEach { transmisjon ->
                            try {
                                dialogportenClient.replaceTransmission(
                                    dialog.dialogId,
                                    transmisjon.id.value,
                                    sykepengesoknadTransmission(transmisjon.dokumentId, isSilentUpdate = true).toTransmission(),
                                )
                                antallOppdatert++
                            } catch (e: Exception) {
                                antallFeilet++
                                logger.error(
                                    "Klarte ikke å fikse søknad-transmission ${transmisjon.id.value} " +
                                        "i dialog ${dialog.dialogId} for dokumentId ${transmisjon.dokumentId}",
                                    e,
                                )
                            }
                        }
                }
            }

        logger.info("Ferdig med å fikse transmission-urler. Oppdatert $antallOppdatert, feilet $antallFeilet.")
    }
}
