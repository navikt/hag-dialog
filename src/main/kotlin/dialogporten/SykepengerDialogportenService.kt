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
import org.jetbrains.exposed.exceptions.ExposedSQLException
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

    suspend fun fixManglendeSykmeldinger() {
        val foersteDag = LocalDate.of(2026, 1, 5)
        val sisteDagInklusiv = LocalDate.of(2026, 5, 28)
        var antallOpprettet = 0
        var antallFeilet = 0

        logger.info("Starter å fikse manglende sykmelding transmission fra og med $foersteDag til og med $sisteDagInklusiv")

        generateSequence(foersteDag) { it.plusDays(1) }
            .takeWhile { !it.isAfter(sisteDagInklusiv) }
            .forEach { dag ->
                val dialoger = dialogRepository.hentDialogerOpprettetPaaDag(dag)
                logger.info("Fant ${dialoger.size} dialoger opprettet $dag")
                val oppretterStart = antallOpprettet
                val feiletStart = antallFeilet

                dialoger.forEach { dialog ->
                    try {
                        val opprettet = opprettManglendeTranmissionSykmelding(dialog.dialogId, dialog.sykmeldingId)
                        if (opprettet) antallOpprettet++ else antallFeilet++
                    } catch (e: Exception) {
                        logger.error("sykmelding for ${dialog.dialogId} feilet", e)
                        antallFeilet++
                    }
                }
                logger.info("Ferdig med $dag. Opprettet: ${antallOpprettet - oppretterStart}, feilet: ${antallFeilet - feiletStart}.")
            }
    }

    suspend fun opprettManglendeTranmissionSykmelding(
        dialogId: UUID,
        sykmeldingId: UUID,
    ): Boolean {
        val dialog = dialogportenClient.getDialog(dialogId)
        if (dialog.isFailure) {
            logger.error("Henting av dialog $dialogId feilet", dialog.exceptionOrNull())
            return false
        }

        val sykmeldingTransmission =
            dialog
                .getOrNull()
                ?.transmissions
                .orEmpty()
                .firstOrNull { it.extendedType == LpsApiExtendedType.SYKMELDING.toString() }

        if (sykmeldingTransmission == null) {
            logger.warn("Fant ingen sykmelding-transmission for dialog $dialogId")
            return false
        }

        val transmissionId = sykmeldingTransmission.id
        if (transmissionId == null) {
            logger.warn("Sykmelding transmission uten id for dialog $dialogId")
            return false
        }

        try {
            dialogRepository.oppdaterDialogMedTransmission(
                sykmeldingId = sykmeldingId,
                transmissionId = transmissionId,
                dokumentId = sykmeldingId,
                dokumentType = LpsApiExtendedType.SYKMELDING.toString(),
                relatedTransmissionId = sykmeldingTransmission.relatedTransmissionId,
            )
        } catch (e: ExposedSQLException) {
            logger.info("DB feil, transmission $transmissionId finnes sansynligvis allerede for dialog $dialogId")
        }
        return true
    }
}
