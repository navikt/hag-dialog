package no.nav.helsearbeidsgiver.database

import no.nav.helsearbeidsgiver.kafka.FritakKravMelding
import no.nav.helsearbeidsgiver.kafka.FritakKravStatus
import no.nav.helsearbeidsgiver.kafka.GravidKravMelding
import no.nav.helsearbeidsgiver.kafka.KroniskKravMelding
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object FritakAgpKravTable : UUIDTable("fritakagp_krav", "transmission_id") {
    val dialogId = uuid("dialog_id")
    val kravId = uuid("krav_id")
    val kravType = enumerationByName<FritakAgpType>("krav_type", 50)
    val fnr = varchar("fnr", 50)
    val orgnr = varchar("orgnr", 50)
    val opprettet = datetime("opprettet").clientDefault { LocalDateTime.now() }
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
        is GravidKravMelding -> {
            when (kravMelding.status) {
                FritakKravStatus.OPPRETTET -> FritakAgpType.GRAVID_KRAV_OPPRETTET
                FritakKravStatus.ENDRET -> FritakAgpType.GRAVID_KRAV_ENDRET
                FritakKravStatus.SLETTET -> FritakAgpType.GRAVID_KRAV_SLETTET
            }
        }

        is KroniskKravMelding -> {
            when (kravMelding.status) {
                FritakKravStatus.OPPRETTET -> FritakAgpType.KRONISK_KRAV_OPPRETTET
                FritakKravStatus.ENDRET -> FritakAgpType.KRONISK_KRAV_ENDRET
                FritakKravStatus.SLETTET -> FritakAgpType.KRONISK_KRAV_SLETTET
            }
        }
    }
