package no.nav.helsearbeidsgiver.dokumentkobling

import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository

private const val ANTALL_MINUTTER_FOER_TIDSAVBRUDD = 60L

class AvbrytInntektsmeldingJobb(
    dokumentkoblingRepository: DokumentkoblingRepository,
) : AvbrytDokumentJobb(
        dokumentNavn = "inntektsmeldinger",
        antallMinutterFoerTidsavbrudd = ANTALL_MINUTTER_FOER_TIDSAVBRUDD,
        settTilTidsavbrutt = dokumentkoblingRepository::settInntektsmeldingerMedStatusMottattTilTidsavbrutt,
    )
