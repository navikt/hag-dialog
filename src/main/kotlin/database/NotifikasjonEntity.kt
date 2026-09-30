package no.nav.helsearbeidsgiver.database

import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.Tjeneste
import no.nav.helsearbeidsgiver.notifikasjon.NotifikasjonStatus
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID

object NotifikasjonTable : UUIDTable(name = "notifikasjon", columnName = "notifikasjon_id") {
    val notifikasjonId get() = id
    val dokumentId = uuid("dokument_id")
    val tjeneste = enumerationByName(name = "tjeneste", length = 50, klass = Tjeneste::class)
    val status = enumerationByName(name = "status", length = 50, klass = NotifikasjonStatus::class)
    val opprettet = datetime("opprettet").clientDefault { LocalDateTime.now() }
}

class NotifikasjonEntity(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<NotifikasjonEntity>(NotifikasjonTable)

    val dokumentId by NotifikasjonTable.dokumentId
    val tjeneste by NotifikasjonTable.tjeneste
    val status by NotifikasjonTable.status
    val opprettet by NotifikasjonTable.opprettet
}

data class Notifikasjon(
    val notifikasjonId: UUID,
    val dokumentId: UUID,
    val tjeneste: Tjeneste,
)
