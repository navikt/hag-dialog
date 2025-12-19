package no.nav.helsearbeidsgiver.dokumentkobling

import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository

private const val ANTALL_MINUTTER_FOER_TIDSAVBRUDD = 60L

class AvbrytSykepengeSoeknadJobb(
    dokumentkoblingRepository: DokumentkoblingRepository,
) : AvbrytDokumentJobb(
        dokumentNavn = "sykepengesøknader",
        antallMinutterFoerTidsavbrudd = ANTALL_MINUTTER_FOER_TIDSAVBRUDD,
        settTilTidsavbrutt = dokumentkoblingRepository::settSykepengeSoeknaderMedStatusMottattTilTidsavbrutt,
    )
