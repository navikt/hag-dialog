package no.nav.helsearbeidsgiver.database

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID

object TransmissionTable : UUIDTable("transmission") {
    val dialogId = reference("dialog_id", DialogTable)
    val dokumentId = uuid("dokument_id")
    val dokumentType = varchar("dokument_type", 50)
    val relatedTransmissionId = uuid("related_transmission_id").nullable()
    val opprettet = datetime("opprettet").clientDefault { LocalDateTime.now() }
}

class TransmissionEntity(
    id: EntityID<UUID>,
) : Entity<UUID>(id) {
    companion object : EntityClass<UUID, TransmissionEntity>(TransmissionTable)

    val dialog by DialogEntity referencedOn TransmissionTable.dialogId
    val dokumentId by TransmissionTable.dokumentId
    val dokumentType by TransmissionTable.dokumentType
    val relatedTransmissionId by TransmissionTable.relatedTransmissionId
    val opprettet by TransmissionTable.opprettet
}
