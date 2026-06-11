@file:UseSerializers(LocalDateSerializer::class, UuidSerializer::class)

package no.nav.helsearbeidsgiver.kafka

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.tilNorskFormat
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.time.LocalDate
import java.util.UUID

@Serializable
sealed class Melding

@Serializable
@SerialName("Sykmelding")
data class Sykmelding(
    val sykmeldingId: UUID,
    val orgnr: Orgnr,
    val foedselsdato: LocalDate,
    val fulltNavn: String,
    val sykmeldingsperioder: List<Sykmeldingsperiode>,
) : Melding()

@Serializable
@SerialName("Sykepengesoeknad")
data class Sykepengesoeknad(
    val soeknadId: UUID,
    val sykmeldingId: UUID,
    val orgnr: Orgnr,
) : Melding()

@Serializable
@SerialName("Inntektsmeldingsforespoersel")
data class Inntektsmeldingsforespoersel(
    val forespoerselId: UUID,
    val sykmeldingId: UUID,
    val orgnr: Orgnr,
) : Melding()

@Serializable
@SerialName("UtgaattInntektsmeldingForespoersel")
data class UtgaattInntektsmeldingForespoersel(
    val forespoerselId: UUID,
    val sykmeldingId: UUID,
    val orgnr: Orgnr,
) : Melding()

@Serializable
data class Sykmeldingsperiode(
    val fom: LocalDate,
    val tom: LocalDate,
)

@Serializable
@SerialName("Inntektsmelding")
data class Inntektsmelding(
    val forespoerselId: UUID,
    val innsendingId: UUID,
    val sykmeldingId: UUID,
    val orgnr: Orgnr,
    val status: Status,
    val kanal: Kanal,
) : Melding() {
    @Serializable
    enum class Status {
        GODKJENT,
        FEILET,
    }

    enum class Kanal {
        NAV_NO,
        HR_SYSTEM_API,
    }
}

fun List<Sykmeldingsperiode>.getSykmeldingsPerioderString(): String =
    when (size) {
        1 -> {
            "sykmeldingsperiode ${first().fom.tilNorskFormat()} – ${first().tom.tilNorskFormat()}"
        }

        else -> {
            "sykmeldingsperioder ${first().fom.tilNorskFormat()} – (...) – ${last().tom.tilNorskFormat()}"
        }
    }

fun List<Sykmeldingsperiode>.lagDialogSummary(): String =
    """
    Gjelder ${getSykmeldingsPerioderString()}
    NAV samler nå sykmelding, søknad og inntektsmelding i én felles melding i Altinn-innboksen. Meldingen inneholder alltid sykmelding. Søknad om sykepenger blir tilgjengelig når den ansatte har søkt, og du kan sende inntektsmelding herfra når Nav har behov for det. Meldingen blir oppdatert automatisk. 
    Fra og med 15. juni 2026 vises sykmelding og søknad kun her, og ikke lenger som separate meldinger i innboksen.    
    """.trimIndent()
