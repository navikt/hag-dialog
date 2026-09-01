package no.nav.helsearbeidsgiver.database

import dokumentkobling.Status
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID

object VedtakTable : UUIDTable(name = "vedtak", columnName = "vedtak_id") {
    val vedtakId get() = id
    val sykmeldingId = uuid("sykmelding_id")
    val inntektsmeldingId = uuid("inntektsmelding_id")
    val orgnr = varchar("orgnr", 9)
    val status = enumerationByName(name = "status", length = 50, klass = Status::class)
    val opprettet = datetime("opprettet").clientDefault { LocalDateTime.now() }
}

class VedtakEntity(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<VedtakEntity>(VedtakTable)

    val vedtakId: UUID get() = id.value
    val sykmeldingId by VedtakTable.sykmeldingId
    val inntektsmeldingId by VedtakTable.inntektsmeldingId
    val orgnr by VedtakTable.orgnr
    val status by VedtakTable.status
    val opprettet by VedtakTable.opprettet
}
