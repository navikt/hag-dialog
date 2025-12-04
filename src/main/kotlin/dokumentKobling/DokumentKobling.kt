@file:UseSerializers(LocalDateSerializer::class, UuidSerializer::class)

package no.nav.helsearbeidsgiver.dokumentKobling

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.time.LocalDate
import java.util.UUID

@Serializable
sealed class DokumentKobling

@Serializable
@SerialName("Sykmelding")
data class Sykmelding(
    val sykmeldingId: UUID,
    val orgnr: Orgnr,
    val foedselsdato: LocalDate,
    val fulltNavn: String,
    val sykmeldingsperioder: List<Sykmeldingsperiode>,
) : DokumentKobling()

@Serializable
data class Sykmeldingsperiode(
    val fom: LocalDate,
    val tom: LocalDate,
)

@Serializable
@SerialName("Sykepengesoeknad")
data class Sykepengesoeknad(
    val soeknadId: UUID,
    val sykmeldingId: UUID,
    val orgnr: Orgnr,
) : DokumentKobling()

@Serializable
@SerialName("VedtaksperiodeSoeknadKobling")
data class VedtaksperiodeSoeknadKobling(
    val vedtaksperiodeId: UUID,
    val soeknadId: UUID,
) : DokumentKobling()

@Serializable
@SerialName("ForespoerselSendt")
data class ForespoerselSendt(
    val forespoerselId: UUID,
    val vedtaksperiodeId: UUID,
    val orgnr: Orgnr,
) : DokumentKobling()

@Serializable
@SerialName("ForespoerselUtgaatt")
data class ForespoerselUtgaatt(
    val forespoerselId: UUID,
    val vedtaksperiodeId: UUID,
    val orgnr: Orgnr,
) : DokumentKobling()
