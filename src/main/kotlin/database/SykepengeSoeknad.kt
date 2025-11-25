package no.nav.helsearbeidsgiver.database



import java.time.LocalDateTime
import java.util.UUID
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime


object SykepengesoeknadTable : UUIDTable("sykepengesoeknad", "soeknad_id") {
    val sykmeldingId = uuid("sykmelding_id")
    val status = enumerationByName("status", 50, Status::class)
    val opprettet = datetime("opprettet").clientDefault { LocalDateTime.now() }
}

class SykepengesoeknadEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<SykepengesoeknadEntity>(SykepengesoeknadTable)

    val soeknadId: UUID
        get() = id.value
    val sykmeldingId by SykepengesoeknadTable.sykmeldingId
    val status by SykepengesoeknadTable.status
    val opprettet by SykepengesoeknadTable.opprettet
}