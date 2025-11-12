@file:UseSerializers(LocalDateSerializer::class, UuidSerializer::class)

package no.nav.helsearbeidsgiver.kafka

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
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
@SerialName("OppdatertInntektsmeldingsforespoersel")
data class OppdatertInntektsmeldingsforespoersel(
    val forespoerselId: UUID,
    val sykmeldingId: UUID,
    val orgnr: Orgnr,
    val utgaatForespoerselId: UUID,
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
) : Melding() {
    @Serializable
    enum class Status {
        MOTTATT,
        GODKJENT,
        FEILET,
    }
}
