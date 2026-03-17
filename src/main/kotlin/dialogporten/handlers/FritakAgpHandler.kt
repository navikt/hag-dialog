package no.nav.helsearbeidsgiver.dialogporten.handlers

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.domene.CreateDialogRequest
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.util.UUID

class FritakAgpHandler(
    private val dialogportenClient: DialogportenClient,
) {
    fun opprettOgLagreDialog() {
        runBlocking {
            dialogportenClient.createDialog(
                CreateDialogRequest(
                    orgnr = Orgnr("214398982"),
                    externalReference = "fritak-agp",
                    idempotentKey = UUID.randomUUID().toString(),
                    title = "Søknad om fritak fra arbeidsgiverperioden grunnet graviditet.",
                    summary =
                        "Kvittering for mottatt søknad om fritak fra" +
                            " arbeidsgiverperioden grunnet risiko for høyt sykefravær knyttet til graviditet.",
                    transmissions = emptyList(),
                ),
            )
        }
    }
}
