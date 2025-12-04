package no.nav.helsearbeidsgiver.database

import no.nav.helsearbeidsgiver.dokumentKobling.Status
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID

object ForespoerselTable : UUIDTable(name = "forespoersel", columnName = "forespoersel_id") {
    val vedtaksperiodeId = uuid("vedtaksperiode_id")
    val status = enumerationByName(name = "status", length = 50, klass = Status::class)
    val forespoerselStatus = enumerationByName(name = "forespoersel_status", length = 50, klass = ForespoerselStatus::class)
    val opprettet = datetime("opprettet").clientDefault { LocalDateTime.now() }
}

class ForespoerselEntity(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ForespoerselEntity>(ForespoerselTable)

    val forespoerselId: UUID
        get() = id.value
    val vedtaksperiodeId by ForespoerselTable.vedtaksperiodeId
    val status by ForespoerselTable.status
    val forespoerselStatus by ForespoerselTable.forespoerselStatus
    val opprettet by ForespoerselTable.opprettet
}

enum class ForespoerselStatus {
    SENDT,
    UTGAATT,
}
