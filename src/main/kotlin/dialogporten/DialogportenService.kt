package no.nav.helsearbeidsgiver.dialogporten

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.DialogRepository
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.kafka.Sykepengesoknad
import no.nav.helsearbeidsgiver.kafka.Sykmelding
import no.nav.helsearbeidsgiver.kafka.Sykmeldingsperiode
import no.nav.helsearbeidsgiver.utils.json.fromJson
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.tilNorskFormat
import java.util.UUID

class DialogportenService(
    private val dialogportenClient: DialogportenClient,
    private val dialogRepository: DialogRepository,
) {
    private val logger = logger()

    fun opprettOgLagreDialog(sykmelding: Sykmelding) {
        val dialogId = opprettNyDialogMedSykmelding(sykmelding)
        dialogRepository.lagreDialog(dialogId = dialogId, sykmeldingId = sykmelding.sykmeldingId)
    }

    fun oppdaterDialog(sykepengesoknad: Sykepengesoknad) {
        val dialogId = dialogRepository.finnDialogId(sykmeldingId = sykepengesoknad.sykmeldingId)
        if (dialogId == null) {
            logger.warn(
                "Fant ikke dialog for sykmeldingId ${sykepengesoknad.sykmeldingId}. " +
                        "Klarer derfor ikke oppdatere dialogen med sykepengesøknad ${sykepengesoknad.soknadId}.",
            )
        } else {
            oppdaterDialogMedSykepengesoknad(dialogId = dialogId, sykepengesoknad = sykepengesoknad)
        }
    }

    private fun opprettNyDialogMedSykmelding(sykmelding: Sykmelding): UUID =
        runBlocking {
            dialogportenClient
                .opprettDialogMedSykmelding(
                    orgnr = sykmelding.orgnr.toString(),
                    dialogTittel = "Sykepenger for ${sykmelding.fulltNavn} (f. ${sykmelding.foedselsdato.tilNorskFormat()})",
                    dialogSammendrag = sykmelding.sykmeldingsperioder.getSykmeldingsPerioderString(),
                    sykmeldingId = sykmelding.sykmeldingId,
                    sykmeldingJsonUrl = "${Env.Nav.arbeidsgiverApiBaseUrl}/sykmelding/${sykmelding.sykmeldingId}",
                )
        }.fromJson(UuidSerializer)

    private fun oppdaterDialogMedSykepengesoknad(
        dialogId: UUID,
        sykepengesoknad: Sykepengesoknad,
    ) {
        runBlocking {
            dialogportenClient.oppdaterDialogMedSykepengesoknad(
                dialogId = dialogId,
                soknadJsonUrl = "${Env.Nav.arbeidsgiverApiBaseUrl}/soknad/${sykepengesoknad.soknadId}",
            )
        }
    }

    private fun List<Sykmeldingsperiode>.getSykmeldingsPerioderString(): String =
        when (size) {
            1 -> "Sykmeldingsperiode ${first().fom.tilNorskFormat()} – ${first().tom.tilNorskFormat()}"
            else ->
                "Sykmeldingsperioder ${first().fom.tilNorskFormat()} – (...) – ${last().tom.tilNorskFormat()}"
        }
}
