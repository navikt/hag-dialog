package no.nav.helsearbeidsgiver.dialogporten

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.kafka.Sykepengesoknad
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

    fun oppdaterDialogMedSykepengesoknad(sykepengesoknad: Sykepengesoknad) {
        runBlocking {
            dialogportenClient.oppdaterDialogMedSykepengesoknad(
                dialogId = UUID.randomUUID(), // TODO: Hent dialogId fra database,
                soknadJsonUrl = "${Env.navArbeidsgiverApiBaseUrl}/soknad/${sykepengesoknad.soknadId}",
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
