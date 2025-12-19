package no.nav.helsearbeidsgiver.dokumentkobling

import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository

private const val ANTALL_MINUTTER_FOER_TIDSAVBRUDD = 60L

class AvbrytSykmeldingJobb(
    dokumentkoblingRepository: DokumentkoblingRepository,
) : AvbrytDokumentJobb(
        dokumentNavn = "sykmeldinger",
        antallMinutterFoerTidsavbrudd = ANTALL_MINUTTER_FOER_TIDSAVBRUDD,
        settTilTidsavbrutt = dokumentkoblingRepository::settSykmeldingerMedStatusMottattTilTidsavbrutt,
    )
