package no.nav.helsearbeidsgiver.database

import dokumentkobling.InnsendingType
import dokumentkobling.Status
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID

object InntektsmeldingTable : UUIDTable(name = "inntektsmelding", columnName = "id") {
    val forespoerselId = uuid("forespoersel_id")
    val vedtaksperiodeId = uuid("vedtaksperiode_id")
    val status = enumerationByName(name = "status", length = 50, klass = Status::class)
    val inntektsmeldingStatus = enumerationByName(name = "inntektsmelding_status", length = 50, klass = InntektsmeldingStatus::class)
    val innsendingType = enumerationByName(name = "innsending_type", length = 50, klass = InnsendingType::class)
    val opprettet = datetime("opprettet").clientDefault { LocalDateTime.now() }
}

class InntektsmeldingEntity(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<InntektsmeldingEntity>(InntektsmeldingTable)

    val forespoerselId by InntektsmeldingTable.forespoerselId
    val vedtaksperiodeId by InntektsmeldingTable.vedtaksperiodeId
    val status by InntektsmeldingTable.status
    val inntektsmeldingStatus by InntektsmeldingTable.inntektsmeldingStatus
    val innsendingType by InntektsmeldingTable.innsendingType
    val opprettet by InntektsmeldingTable.opprettet
}

enum class InntektsmeldingStatus {
    GODKJENT,
    AVVIST,
}
