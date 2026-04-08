@file:UseSerializers(LocalDateSerializer::class, UuidSerializer::class)

package no.nav.helsearbeidsgiver.kafka

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.util.UUID

@Serializable
sealed class DialogMelding

@Serializable
sealed class FritakKravMelding : DialogMelding()

@Serializable
sealed class FritakSoeknadMelding : DialogMelding()

@Serializable
@SerialName("GravidSoeknadMelding")
data class GravidSoeknadMelding(
    val id: UUID,
    val orgnr: Orgnr,
    val navn: String,
    val fnr: String,
) : FritakSoeknadMelding()

@Serializable
@SerialName("KroniskSoeknadMelding")
data class KroniskSoeknadMelding(
    val id: UUID,
    val orgnr: Orgnr,
    val navn: String,
    val fnr: String,
) : FritakSoeknadMelding()

@Serializable
@SerialName("GravidKravMelding")
data class GravidKravMelding(
    val id: UUID,
    val orgnr: Orgnr,
    val navn: String,
    val fnr: String,
    val status: FritakKravStatus,
) : FritakKravMelding()

@Serializable
@SerialName("KroniskKravMelding")
data class KroniskKravMelding(
    val id: UUID,
    val orgnr: Orgnr,
    val navn: String,
    val fnr: String,
    val status: FritakKravStatus,
) : FritakKravMelding()

enum class FritakKravStatus(
    val verdi: String,
) {
    OPPRETTET("opprettet"),
    ENDRET("endret"),
    SLETTET("slettet"),
}

fun foedselsdatoFraFnr(fnr: String): String = fnr.take(6)
