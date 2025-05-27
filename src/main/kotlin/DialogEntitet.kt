package no.nav.helsearbeidsgiver

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object DialogEntitet : Table("dialog") {
    val id = ulong("id").autoIncrement()
    val dialogId = uuid("dialog_id")
    val sykmeldingId = uuid("sykmelding_id")
    val opprettet = datetime("opprettet")
}
