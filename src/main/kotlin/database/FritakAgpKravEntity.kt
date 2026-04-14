package no.nav.helsearbeidsgiver.database

import no.nav.helsearbeidsgiver.kafka.FritakKravMelding
import no.nav.helsearbeidsgiver.kafka.GravidKrav
import no.nav.helsearbeidsgiver.kafka.GravidKravEndret
import no.nav.helsearbeidsgiver.kafka.GravidKravSlettet
import no.nav.helsearbeidsgiver.kafka.KroniskKrav
import no.nav.helsearbeidsgiver.kafka.KroniskKravEndret
import no.nav.helsearbeidsgiver.kafka.KroniskKravSlettet
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID

object FritakAgpKravTable : UUIDTable("fritakagp_krav", "transmission_id") {
    val dialogId = uuid("dialog_id")
    val kravId = uuid("krav_id")
    val kravType = enumerationByName<FritakAgpType>("krav_type", 50)
    val fnr = varchar("fnr", 50)
    val orgnr = varchar("orgnr", 50)
    val opprettet = datetime("opprettet").clientDefault { LocalDateTime.now() }
}

class FritakAgpKravEntity(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<FritakAgpKravEntity>(FritakAgpKravTable)

    val transmissionId get() = id.value
    val dialogId by FritakAgpKravTable.dialogId
    val kravId by FritakAgpKravTable.kravId
    val kravType by FritakAgpKravTable.kravType
    val fnr by FritakAgpKravTable.fnr
    val orgnr by FritakAgpKravTable.orgnr
    val opprettet by FritakAgpKravTable.opprettet
}

enum class FritakAgpType {
    GRAVID_KRAV_OPPRETTET,
    GRAVID_KRAV_ENDRET,
    GRAVID_KRAV_SLETTET,

    KRONISK_KRAV_OPPRETTET,
    KRONISK_KRAV_ENDRET,
    KRONISK_KRAV_SLETTET,
}

fun finnTypeForFritakKrav(kravMelding: FritakKravMelding): FritakAgpType =
    when (kravMelding) {
        is GravidKrav -> FritakAgpType.GRAVID_KRAV_OPPRETTET
        is GravidKravEndret -> FritakAgpType.GRAVID_KRAV_ENDRET
        is GravidKravSlettet -> FritakAgpType.GRAVID_KRAV_SLETTET
        is KroniskKrav -> FritakAgpType.KRONISK_KRAV_OPPRETTET
        is KroniskKravEndret -> FritakAgpType.KRONISK_KRAV_ENDRET
        is KroniskKravSlettet -> FritakAgpType.KRONISK_KRAV_SLETTET
    }
