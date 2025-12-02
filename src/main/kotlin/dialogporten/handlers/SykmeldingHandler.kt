package no.nav.helsearbeidsgiver.dialogporten.handlers

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.SykmeldingTransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.CreateDialogRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.Transmission
import no.nav.helsearbeidsgiver.dialogporten.domene.addAttachment
import no.nav.helsearbeidsgiver.dialogporten.domene.createApiAttachment
import no.nav.helsearbeidsgiver.dialogporten.domene.toTransmission
import no.nav.helsearbeidsgiver.kafka.Sykmelding
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
                                sykmeldingTransmission(sykmelding),
                            ),
                        isApiOnly = unleashFeatureToggles.skalOppretteDialogKunForApi(),
                    )

                dialogportenClient.createDialog(request)
            }
        dialogRepository.lagreDialog(dialogId = dialogId, sykmeldingId = sykmelding.sykmeldingId)
        logger.info("Opprettet dialog $dialogId for sykmelding ${sykmelding.sykmeldingId}.")
    }
}

fun sykmeldingTransmission(sykmelding: Sykmelding): Transmission =
    SykmeldingTransmissionRequest(sykmelding).toTransmission().addAttachment(
        createApiAttachment(
            displayName = "sykmelding.json",
            url = "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/sykmelding/${sykmelding.sykmeldingId}",
        ),
    )
