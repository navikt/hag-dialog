package no.nav.helsearbeidsgiver.dialogporten.handlers

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.SykmeldingTransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.CreateDialogRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.lagTransmissionMedVedlegg
import no.nav.helsearbeidsgiver.dokumentKobling.Sykmelding
import no.nav.helsearbeidsgiver.dokumentKobling.getSykmeldingsPerioderString
import no.nav.helsearbeidsgiver.kafka.getSykmeldingsPerioderString
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.tilNorskFormat

class SykmeldingHandler(
    private val dialogRepository: DialogRepository,
    private val dialogportenClient: DialogportenClient,
    private val unleashFeatureToggles: UnleashFeatureToggles,
) {
    private val logger = logger()

    fun opprettOgLagreDialog(sykmelding: Sykmelding) {
        val dialogId =
            runBlocking {
                val request =
                    CreateDialogRequest(
                        orgnr = sykmelding.orgnr,
                        externalReference = sykmelding.sykmeldingId.toString(),
                        idempotentKey = sykmelding.sykmeldingId.toString(),
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
                    )

                dialogportenClient.createDialog(request)
            }
        dialogRepository.lagreDialog(dialogId = dialogId, sykmeldingId = sykmelding.sykmeldingId)
        logger.info("Opprettet dialog $dialogId for sykmelding ${sykmelding.sykmeldingId}.")
    }
}
