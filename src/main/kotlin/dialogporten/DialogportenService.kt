package no.nav.helsearbeidsgiver.dialogporten

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.kafka.Sykmelding
import no.nav.helsearbeidsgiver.kafka.Sykmeldingsperiode
import no.nav.helsearbeidsgiver.utils.tilNorskFormat
import java.util.UUID

class DialogportenService(
    private val dialogportenClient: DialogportenClient,
) {
    fun opprettNyDialogMedSykmelding(sykmelding: Sykmelding): String =
        runBlocking {
            dialogportenClient
                .opprettDialogMedSykmelding(
                    orgnr = sykmelding.orgnr.toString(),
                    dialogTittel = "Sykepenger for ${sykmelding.fulltNavn} (f. ${sykmelding.foedselsdato.tilNorskFormat()})",
                    dialogSammendrag = sykmelding.sykmeldingsperioder.getSykmeldingsPerioderString(),
                    sykmeldingId = sykmelding.sykmeldingId,
                    sykmeldingJsonUrl = "${Env.navArbeidsgiverApiBaseUrl}/sykmelding/${sykmelding.sykmeldingId}",
                )
        }

    fun oppdaterDialogMedSoknad(sykmelding: Sykmelding) { // TODO: Endre tilbake til soknad
        // val dialogId = dialogDao.hentDialogId(soknad.sykmeldingId)
        val dialogId = UUID.fromString("0196c429-124f-75c5-a7e7-61735a9ca051") // TODO: Hente dialogId fra database
        runBlocking {
            dialogportenClient.oppdaterDialogMedSoknad(
                dialogId = dialogId,
                soknadJsonUrl = "${Env.navArbeidsgiverApiBaseUrl}/soknad/${sykmelding.sykmeldingId}", // TODO: Bruk soknadId fra melding
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
