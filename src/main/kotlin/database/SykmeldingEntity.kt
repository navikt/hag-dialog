package no.nav.helsearbeidsgiver.database

import java.time.LocalDateTime
import java.util.UUID
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime


enum class Status {
    MOTATT,
    BEHANDLET,
}

object SykmeldingTable : UUIDTable("sykmelding", columnName = "sykmelding_id") {
    val status = enumerationByName("status", 50, Status::class)
    val opprettet = datetime("opprettet").clientDefault { LocalDateTime.now() }
}

class SykmeldingEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : EntityClass<UUID, SykmeldingEntity>(SykmeldingTable)

    val status by SykmeldingTable.enumerationByName("status", 50, Status::class)
    val opprettet by SykmeldingTable.opprettet
}