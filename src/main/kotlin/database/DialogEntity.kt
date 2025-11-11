package no.nav.helsearbeidsgiver.database

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID

object DialogTable : UUIDTable("dialog") {
    val sykmeldingId = uuid("sykmelding_id")
    val opprettet = datetime("opprettet").clientDefault { LocalDateTime.now() }
}

class DialogEntity(
    id: EntityID<UUID>,
) : Entity<UUID>(id) {
    companion object : EntityClass<UUID, DialogEntity>(DialogTable)

    val dialogId: UUID
        get() = id.value
    var sykmeldingId by DialogTable.sykmeldingId
    var opprettet by DialogTable.opprettet
    val transmissions by TransmissionEntity referrersOn TransmissionTable.dialogId

    fun transmissionByDokumentId(dokumentId: UUID): TransmissionEntity? =
        transaction {
            transmissions.firstOrNull {
                it.dokumentId ==
                    dokumentId
            }
        }

    fun transmissionsByDokumentType(dokumentType: String): List<TransmissionEntity> =
        transaction {
            transmissions.filter { it.dokumentType == dokumentType }
        }
}
