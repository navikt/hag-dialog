@file:UseSerializers(LocalDateSerializer::class, UuidSerializer::class)

package no.nav.helsearbeidsgiver.kafka

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
data class Sykmelding(
    val sykmeldingId: UUID,
    val orgnr: Orgnr,
    val foedselsdato: LocalDate,
    val fulltNavn: String,
    val sykmeldingsperioder: List<Sykmeldingsperiode>,
) : Melding()

@Serializable
data class Sykepengesoeknad(
    val soeknadId: UUID,
    val sykmeldingId: UUID,
    val orgnr: Orgnr,
) : Melding()

@Serializable
data class Inntektsmeldingsforespoersel(
    val forespoerselId: UUID,
    val sykmeldingId: UUID,
    val orgnr: Orgnr,
) : Melding()

@Serializable
data class Sykmeldingsperiode(
    val fom: LocalDate,
    val tom: LocalDate,
)
