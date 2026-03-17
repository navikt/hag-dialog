package no.nav.helsearbeidsgiver.dialogporten.handlers

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.domene.CreateDialogRequest
import no.nav.helsearbeidsgiver.kafka.DialogMelding
import no.nav.helsearbeidsgiver.kafka.GravidSoeknadMelding
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.util.UUID

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
                    idempotentKey = UUID.randomUUID().toString(),
                    title = "Søknad om fritak fra arbeidsgiverperioden grunnet graviditet.",
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
