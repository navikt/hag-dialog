package no.nav.helsearbeidsgiver.database

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object FritakAgpSoeknadTable : UUIDTable("fritakagp_soeknad", "dialog_id") {
    val soeknadId = uuid("soeknad_id").uniqueIndex()
    val soeknadType = enumerationByName(name = "soeknad_type", length = 50, klass = FritakAgpDokType::class)
    val fnr = varchar("fnr", 50)
    val orgnr = varchar("orgnr", 50)
    val opprettet = datetime("opprettet").clientDefault { LocalDateTime.now() }
}

enum class FritakAgpDokType {
    GRAVID_KRAV,
    GRAVID_SOEKNAD,
    KRONISK_KRAV,
    KRONISK_SOEKNAD,
}
