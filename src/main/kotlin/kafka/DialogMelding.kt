@file:UseSerializers(LocalDateSerializer::class, UuidSerializer::class)

package no.nav.helsearbeidsgiver.kafka

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer

@Serializable
sealed class DialogMelding

@Serializable
data class GravidSoeknadMelding(
    val orgnr: String,
    val sykemeldt: String,
) : DialogMelding()
