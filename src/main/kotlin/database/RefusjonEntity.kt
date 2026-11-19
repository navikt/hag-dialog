package no.nav.helsearbeidsgiver.database

import dokumentkobling.Status
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID

object RefusjonTable : UUIDTable(name = "refusjon", columnName = "refusjon_utfall_id") {
    val refusjonUtfallId get() = id
    val sykmeldingId = uuid("sykmelding_id")
    val inntektsmeldingId = uuid("inntektsmelding_id")
    val orgnr = text("orgnr")
    val fom = date("fom")
    val tom = date("tom")
    val status = enumerationByName(name = "status", length = 50, klass = Status::class)
    val opprettet = datetime("opprettet").clientDefault { LocalDateTime.now() }
}

class RefusjonEntity(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<RefusjonEntity>(RefusjonTable)

    val refusjonUtfallId: UUID get() = id.value
    val sykmeldingId by RefusjonTable.sykmeldingId
    val inntektsmeldingId by RefusjonTable.inntektsmeldingId
    val orgnr by RefusjonTable.orgnr
    val fom by RefusjonTable.fom
    val tom by RefusjonTable.tom
    val status by RefusjonTable.status
    val opprettet by RefusjonTable.opprettet
}
