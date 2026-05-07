package no.nav.helsearbeidsgiver.database

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object FritakAgpSoeknadTable : UUIDTable("fritakagp_soeknad", "dialog_id") {
    val soeknadId = uuid("soeknad_id").uniqueIndex()
    val soeknadType = enumerationByName(name = "soeknad_type", length = 50, klass = FritakAgpSoeknadType::class)
    val fnr = varchar("fnr", 11)
    val orgnr = varchar("orgnr", 9)
    val opprettet = datetime("opprettet").clientDefault { LocalDateTime.now() }
}

enum class FritakAgpSoeknadType {
    GRAVID_SOEKNAD,
    KRONISK_SOEKNAD,
}
