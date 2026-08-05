package no.nav.helsearbeidsgiver.dialogporten

import kotlinx.coroutines.delay
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.dialogporten.domene.Transmission
import no.nav.helsearbeidsgiver.dialogporten.handlers.ForespoerselHandler
import no.nav.helsearbeidsgiver.dialogporten.handlers.InntektsmeldingHandler
import no.nav.helsearbeidsgiver.dialogporten.handlers.SykepengesoeknadHandler
import no.nav.helsearbeidsgiver.dialogporten.handlers.SykmeldingHandler
import no.nav.helsearbeidsgiver.dialogporten.handlers.UtgaattForespoerselHandler
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
                        val opprettet = opprettManglendeTransmissionSykmelding(dialog.dialogId, dialog.sykmeldingId)
                        if (opprettet) antallOpprettet++ else antallFeilet++
                    } catch (e: Exception) {
                        logger.error("sykmelding for ${dialog.dialogId} feilet")
                        antallFeilet++
                    }
                }
                logger.info("Ferdig med $dag. Opprettet: ${antallOpprettet - oppretterStart}, feilet: ${antallFeilet - feiletStart}.")
            }
    }

    suspend fun opprettManglendeTransmissionSykmelding(
        dialogId: UUID,
        sykmeldingId: UUID,
    ): Boolean {
        delay(10.milliseconds)

        val sykmeldingTransmission = hentTransmissionId(dialogId) ?: return false

        return oppdaterDialogMedTransmission(dialogId, sykmeldingId, sykmeldingTransmission)
    }

    private suspend fun hentTransmissionId(dialogId: UUID): Transmission? {
        val dialog = dialogportenClient.getDialog(dialogId)
        if (dialog.isFailure) {
            logger.error("Henting av dialog $dialogId feilet", dialog.exceptionOrNull())
            return null
        }

        val sykmeldingTransmission =
            dialog
                .getOrNull()
                ?.transmissions
                .orEmpty()
                .firstOrNull { it.extendedType == LpsApiExtendedType.SYKMELDING.toString() }

        if (sykmeldingTransmission == null) {
            logger.warn("Fant ingen sykmelding-transmission for dialog $dialogId")
            return null
        }

        if (sykmeldingTransmission.id == null) {
            logger.warn("Sykmelding transmission uten id for dialog $dialogId")
            return null
        }

        return sykmeldingTransmission
    }

    private fun oppdaterDialogMedTransmission(
        dialogId: UUID,
        sykmeldingId: UUID,
        sykmeldingTransmission: Transmission,
    ): Boolean {
        val transmissionId = requireNotNull(sykmeldingTransmission.id)
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
            return false
        }
        return true
    }
}
