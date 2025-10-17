package no.nav.helsearbeidsgiver

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.util.UUID

object DialogEntitet : Table("dialog") {
    val id = ulong("id").autoIncrement()
    val dialogId = uuid("dialog_id")
    val sykmeldingId = uuid("sykmelding_id")
    val forespoerselTransmission = uuid("forespoersel_transmission")
    val opprettet = datetime("opprettet")
}

data class DialogExposed(
    val resultRow: ResultRow,
) {
    val id: Long = resultRow[DialogEntitet.id].toLong()
    val dialogId: UUID = resultRow[DialogEntitet.dialogId]
    val sykmeldingId: UUID = resultRow[DialogEntitet.sykmeldingId]
    val forespoerselTransmission: UUID? = resultRow[DialogEntitet.forespoerselTransmission]
    val opprettet = resultRow[DialogEntitet.opprettet]
}
