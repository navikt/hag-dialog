package no.nav.helsearbeidsgiver.database

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID

object FritakAgpSoeknadTable : UUIDTable("fritakagp_soeknad", "dialog_id") {
    val soeknadId = uuid("soeknad_id").uniqueIndex()
    val soeknadType = enumerationByName(name = "soeknad_type", length = 50, klass = FritakAgpSoeknadType::class)
    val fnr = varchar("fnr", 11)
    val orgnr = varchar("orgnr", 9)
    val opprettet = datetime("opprettet").clientDefault { LocalDateTime.now() }
}

class FritakAgpSoeknadEntity(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object :
        UUIDEntityClass<FritakAgpSoeknadEntity>(FritakAgpSoeknadTable)

    val dialogId get() = id.value
    val soeknadId by FritakAgpSoeknadTable.soeknadId
    val soeknadType by FritakAgpSoeknadTable.soeknadType
    val fnr by FritakAgpSoeknadTable.fnr
    val orgnr by FritakAgpSoeknadTable.orgnr
    val opprettet by FritakAgpSoeknadTable.opprettet
}

enum class FritakAgpSoeknadType {
    GRAVID_SOEKNAD,
    KRONISK_SOEKNAD,
}
