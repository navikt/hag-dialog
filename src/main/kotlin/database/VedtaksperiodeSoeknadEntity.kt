package no.nav.helsearbeidsgiver.database

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID

object VedtaksperiodeSoeknadTable : UUIDTable(name = "vedtaksperiode_soeknad", columnName = "id") {
    val vedtaksperiodeId = uuid("vedtaksperiode_id")
    val soeknadId = uuid("soeknad_id")
    val opprettet = datetime("opprettet").clientDefault { LocalDateTime.now() }
}

class VedtaksperiodeSoeknadEntity(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<VedtaksperiodeSoeknadEntity>(VedtaksperiodeSoeknadTable)

    val vedtaksperiodeId: UUID by VedtaksperiodeSoeknadTable.vedtaksperiodeId
    val soeknadId by VedtaksperiodeSoeknadTable.soeknadId
}
