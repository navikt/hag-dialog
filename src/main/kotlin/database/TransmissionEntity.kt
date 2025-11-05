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
    val relatedTransmission = uuid("related_transmission").nullable()
    val opprettet = datetime("opprettet").clientDefault { LocalDateTime.now() }

    init {
        index("dokument_id_index", false, dokumentId)
    }
}

class TransmissionEntity(
    id: EntityID<UUID>,
) : Entity<UUID>(id) {
    companion object : EntityClass<UUID, TransmissionEntity>(TransmissionTable)

    var dialog by DialogEntity referencedOn TransmissionTable.dialogId
    var dokumentId by TransmissionTable.dokumentId
    var dokumentType by TransmissionTable.dokumentType
    var relatedTransmission by TransmissionTable.relatedTransmission
    var opprettet by TransmissionTable.opprettet
}
