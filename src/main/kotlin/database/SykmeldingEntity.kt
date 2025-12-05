package no.nav.helsearbeidsgiver.database

import dokumentkobling.Status
import dokumentkobling.Sykmelding
import no.nav.helsearbeidsgiver.utils.json.jsonConfig
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.json.jsonb
import java.time.LocalDateTime
import java.util.UUID

object SykmeldingTable : UUIDTable(name = "sykmelding", columnName = "sykmelding_id") {
    val sykmeldingId get() = id
    val status = enumerationByName(name = "status", length = 50, klass = Status::class)
    val data = jsonb<Sykmelding>(name = "data", jsonConfig = jsonConfig, kSerializer = Sykmelding.serializer())
    val opprettet = datetime("opprettet").clientDefault { LocalDateTime.now() }
}

class SykmeldingEntity(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<SykmeldingEntity>(SykmeldingTable)

    val sykmeldingId: UUID get() = sykmeldingId
    val status by SykmeldingTable.status
    val data by SykmeldingTable.data
    val opprettet by SykmeldingTable.opprettet
}
