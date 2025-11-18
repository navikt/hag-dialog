package no.nav.helsearbeidsgiver.dialogporten

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.database.TransmissionTable.dialogId
import no.nav.helsearbeidsgiver.dialogporten.domene.CreateDialogRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.DialogStatus
import no.nav.helsearbeidsgiver.dialogporten.domene.lagTransmissionMedVedlegg
import no.nav.helsearbeidsgiver.kafka.Sykmelding
import no.nav.helsearbeidsgiver.kafka.Sykmeldingsperiode
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.tilNorskFormat
import java.util.UUID

class DialogportenService(
    val dialogRepository: DialogRepository,
    val dialogportenClient: DialogportenClient,
    val unleashFeatureToggles: UnleashFeatureToggles,
) {
    val logger = logger()

    fun opprettOgLagreDialog(sykmelding: Sykmelding) {
        val dialogId = opprettNyDialogMedSykmelding(sykmelding)

        dialogRepository.lagreDialog(dialogId = dialogId, sykmeldingId = sykmelding.sykmeldingId)
        logger.info("Opprettet dialog $dialogId for sykmelding ${sykmelding.sykmeldingId}.")
    }

    private fun opprettNyDialogMedSykmelding(sykmelding: Sykmelding): UUID =
        runBlocking {
            val request =
                CreateDialogRequest(
                    orgnr = sykmelding.orgnr,
                    externalReference = sykmelding.sykmeldingId.toString(),
                    title =
                        "Sykepenger for ${sykmelding.fulltNavn} (f. ${sykmelding.foedselsdato.tilNorskFormat()})",
                    summary =
                        sykmelding.sykmeldingsperioder
                            .getSykmeldingsPerioderString(),
                    transmissions =
                        listOf(
                            lagTransmissionMedVedlegg(
                                SykmeldingTransmissionRequest(sykmelding),
                            ),
                        ),
                    isApiOnly = unleashFeatureToggles.skalOppretteDialogKunForApi(),
                    idempotentKey = sykmelding.sykmeldingId.toString(),
                )

            val dialogId = dialogportenClient.createDialog(request)
            dialogportenClient.setDialogStatus(dialogId, DialogStatus.New)
            dialogId
        }
}

fun List<Sykmeldingsperiode>.getSykmeldingsPerioderString(): String =
    when (size) {
        1 -> "Sykmeldingsperiode ${first().fom.tilNorskFormat()} – ${first().tom.tilNorskFormat()}"
        else ->
            "Sykmeldingsperioder ${first().fom.tilNorskFormat()} – (...) – ${last().tom.tilNorskFormat()}"
    }
