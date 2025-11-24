package no.nav.helsearbeidsgiver.database


import java.time.LocalDateTime
import java.util.UUID
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime


object SykepengesoeknadTable : UUIDTable("sykepengesoeknad", columnName = "soeknad_id") {
    val sykmeldingId = reference("sykmelding_id", SykmeldingTable)
    val status = enumerationByName("status", 50, Status::class)
    val opprettet = datetime("opprettet").clientDefault { LocalDateTime.now() }
}



class SykepengesoeknadEntity(id: EntityID<UUID>) : Entity<UUID>(id) {
    companion object : EntityClass<UUID, SykepengesoeknadEntity>(SykepengesoeknadTable)

    val sykmelding by SykmeldingEntity referencedOn SykepengesoeknadTable.sykmeldingId
//    val status by SykepengesoeknadTable.enumerationByName("status", 50, Status::class)
    val opprettet by SykepengesoeknadTable.opprettet
}