package no.nav.helsearbeidsgiver.database

import dokumentkobling.Status
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID

object SykepengesoeknadTable : UUIDTable(name = "sykepengesoeknad", columnName = "soeknad_id") {
    val soeknadId get() = id
    val sykmeldingId = uuid("sykmelding_id")
    val orgnr = varchar("orgnr", 9)
    val status = enumerationByName(name = "status", length = 50, klass = Status::class)
    val opprettet = datetime("opprettet").clientDefault { LocalDateTime.now() }
}

class SykepengesoeknadEntity(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<SykepengesoeknadEntity>(SykepengesoeknadTable)

    val soeknadId: UUID get() = soeknadId
    val sykmeldingId by SykepengesoeknadTable.sykmeldingId
    val orgnr by SykepengesoeknadTable.orgnr
    val status by SykepengesoeknadTable.status
    val opprettet by SykepengesoeknadTable.opprettet
}
