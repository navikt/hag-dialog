package no.nav.helsearbeidsgiver.database


import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID

object DialogTable : UUIDTable("dialog") {
    val sykmeldingId = uuid("sykmelding_id").uniqueIndex()
    val opprettet = datetime("opprettet").clientDefault { LocalDateTime.now() }

    init {
        index("sykmelding_id_index", true, sykmeldingId)
    }
}

class DialogEntity(
    id: EntityID<UUID>,
) : Entity<UUID>(id) {
    companion object : EntityClass<UUID, DialogEntity>(DialogTable)

    var sykmeldingId by DialogTable.sykmeldingId
    var opprettet by DialogTable.opprettet
    val transmissions by TransmissionEntity referrersOn TransmissionTable.dialogId
}