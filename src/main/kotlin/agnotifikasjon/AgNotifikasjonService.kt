package no.nav.helsearbeidsgiver.notifikasjon

import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.Tjeneste
import no.nav.helsearbeidsgiver.utils.json.toJson
import java.util.UUID

private const val MAKS_ANTALL_FORSOK = 10

class AgNotifikasjonService(
    private val bakgrunnsjobbService: BakgrunnsjobbService,
) {
    fun opprettAgNotifikasjonsJobb(
        dokumentId: UUID,
        tjeneste: Tjeneste,
    ) {
        bakgrunnsjobbService.opprettJobbJson<AgNotifikasjonsJobb>(
            maksAntallForsoek = MAKS_ANTALL_FORSOK,
            data =
                AgNotifikasjon(
                    dokumentId = dokumentId,
                    tjeneste = tjeneste,
                ).toJson(AgNotifikasjon.serializer()),
        )
    }
}
