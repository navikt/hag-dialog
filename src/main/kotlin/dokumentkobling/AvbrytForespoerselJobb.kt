package no.nav.helsearbeidsgiver.dokumentkobling

import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository

private const val ANTALL_MINUTTER_FOER_TIDSAVBRUDD = 60L

class AvbrytForespoerselJobb(
    dokumentkoblingRepository: DokumentkoblingRepository,
) : AvbrytDokumentJobb(
        dokumentNavn = "forespørsler",
        antallMinutterFoerTidsavbrudd = ANTALL_MINUTTER_FOER_TIDSAVBRUDD,
        settTilTidsavbrutt = dokumentkoblingRepository::settForespoerslerMedStatusMottattTilTidsavbrutt,
    )
