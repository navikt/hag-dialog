package no.nav.helsearbeidsgiver.database

import no.nav.helsearbeidsgiver.dokumentKobling.Sykmelding
import no.nav.helsearbeidsgiver.utils.json.jsonConfig
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.json.jsonb
import java.time.LocalDateTime
import java.util.UUID

enum class Status {
    MOTTATT,
    BEHANDLET,
}

object SykmeldingTable : UUIDTable(name = "sykmelding", columnName = "sykmelding_id") {
    val status = enumerationByName("status", 50, Status::class)
    val data = jsonb<Sykmelding>(name = "data", jsonConfig = jsonConfig, kSerializer = Sykmelding.serializer())
    val opprettet = datetime("opprettet").clientDefault { LocalDateTime.now() }
}

class SykmeldingEntity(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<SykmeldingEntity>(SykmeldingTable)

    val sykmeldingId: UUID
        get() = id.value
    val status by SykmeldingTable.status
    val data by SykmeldingTable.data
    val opprettet by SykmeldingTable.opprettet
}
