package no.nav.helsearbeidsgiver.database

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object FritakAgpDialogTable : LongIdTable("fritakagp_dialog") {
    val dialogId = uuid("dialog_id").uniqueIndex()
    val dokumentId = uuid("dokument_id").uniqueIndex()
    val dokumentType = enumerationByName(name = "dokument_type", length = 50, klass = FritakAgpDokType::class)
    val fnr = text("fnr")
    val opprettet = datetime("opprettet").clientDefault { LocalDateTime.now() }
}

class FritakAgpDialogEntity(
    id: EntityID<Long>,
) : LongEntity(id) {
    companion object : LongEntityClass<FritakAgpDialogEntity>(FritakAgpDialogTable)

    val dialogId by FritakAgpDialogTable.dialogId
    val dokumentId by FritakAgpDialogTable.dokumentId
    val dokumentType by FritakAgpDialogTable.dokumentType
    val fnr by FritakAgpDialogTable.fnr
    val opprettet by FritakAgpDialogTable.opprettet
}

enum class FritakAgpDokType {
    GRAVID_KRAV,
    GRAVID_SOEKNAD,
    KRONISK_KRAV,
    KRONISK_SOEKNAD,
}
