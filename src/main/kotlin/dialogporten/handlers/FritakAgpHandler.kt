package no.nav.helsearbeidsgiver.dialogporten.handlers

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.domene.CreateDialogRequest
import no.nav.helsearbeidsgiver.kafka.DialogMelding
import no.nav.helsearbeidsgiver.kafka.GravidSoeknadMelding

class FritakAgpHandler(
    private val dialogportenClient: DialogportenClient,
) {
    fun opprettOgLagreDialog(dialogMelding: DialogMelding) {
        when (dialogMelding) {
            is GravidSoeknadMelding -> opprettDialogForGravidSoeknad(dialogMelding)
        }
    }

    private fun opprettDialogForGravidSoeknad(gravidSoeknadMelding: GravidSoeknadMelding) {
        runBlocking {
            dialogportenClient.createDialog(
                CreateDialogRequest(
                    orgnr = gravidSoeknadMelding.orgnr,
                    externalReference = "fritak-agp",
                    idempotentKey = gravidSoeknadMelding.id.toString(),
                    title = "${sladdFnr(gravidSoeknadMelding.fnr)}-Søknad om fritak fra arbeidsgiverperioden grunnet graviditet.",
                    summary =
                        "Kvittering for mottatt søknad om fritak fra" +
                            " arbeidsgiverperioden grunnet risiko for høyt sykefravær knyttet til graviditet.",
                    transmissions = emptyList(),
                    isApiOnly = false,
                ),
            )
        }
    }
}

fun sladdFnr(fnr: String): String = fnr.take(6) + "*****"
