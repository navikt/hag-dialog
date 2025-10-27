package no.nav.helsearbeidsgiver

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID

object DialogEntitet : Table("dialog") {
    val id = ulong("id").autoIncrement()
    val dialogId = uuid("dialog_id")
    val sykmeldingId = uuid("sykmelding_id")
    val forespoerselTransmissionId = uuid("forespoersel_transmission_id")
    val opprettet = datetime("opprettet")
}

data class DialogExposed(
    val id: ULong,
    val dialogId: UUID,
    val sykmeldingId: UUID,
    val forespoerselTransmission: UUID?,
    val opprettet: LocalDateTime,
)

fun ResultRow.toDialogExposed() =
    DialogExposed(
        this[DialogEntitet.id],
        this[DialogEntitet.dialogId],
        this[DialogEntitet.sykmeldingId],
        this[DialogEntitet.forespoerselTransmissionId],
        this[DialogEntitet.opprettet],
    )
