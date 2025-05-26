import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.DialogEntitet
import no.nav.helsearbeidsgiver.DialogRepository
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class TestRepo(
    private val db: Database,
) {
    fun hentRecordFraDialog(sykmeldingId: UUID): ResultRow? =
        transaction(db) {
            DialogEntitet
                .selectAll()
                .where {
                    DialogEntitet.sykmeldingId eq sykmeldingId
                }.firstOrNull()
        }
}

class DialogRepositoryTest :
    FunSpecWithDb(listOf(DialogEntitet), { db ->
        val testRepo = TestRepo(db)
        val dialogRepo = DialogRepository(db)

        val sykmeldingId = UUID.randomUUID()
        val dialogId = UUID.randomUUID()

        test("lagreDialog skal lagre dialog") {
            transaction {
                DialogEntitet.selectAll().toList()
            }.shouldBeEmpty()
            dialogRepo.lagreDialog(dialogId = dialogId, sykmeldingId = sykmeldingId)

            val record = testRepo.hentRecordFraDialog(sykmeldingId).shouldNotBeNull()
            record.getOrNull(DialogEntitet.dialogId) shouldBe dialogId
        }

        test("finnDialogId skal finne dialogId for sykmeldingId") {
            transaction {
                DialogEntitet.selectAll().toList()
            }.shouldBeEmpty()

            dialogRepo.lagreDialog(dialogId = dialogId, sykmeldingId = sykmeldingId)
            dialogRepo.finnDialogId(sykmeldingId) shouldBe dialogId
        }

        test("lagreDialog skal kaste feil dersom den forsøker å lagre dialog med dialogId som allerede finnes") {
            transaction {
                DialogEntitet.selectAll().toList()
            }.shouldBeEmpty()
            dialogRepo.lagreDialog(dialogId = dialogId, sykmeldingId = sykmeldingId)

            shouldThrow<ExposedSQLException> { dialogRepo.lagreDialog(dialogId = dialogId, sykmeldingId = UUID.randomUUID()) }
        }

        test("lagreDialog skal kaste feil dersom den forsøker å lagre dialog med sykmeldingId som allerede finnes") {
            transaction {
                DialogEntitet.selectAll().toList()
            }.shouldBeEmpty()
            dialogRepo.lagreDialog(dialogId = dialogId, sykmeldingId = sykmeldingId)

            shouldThrow<ExposedSQLException> { dialogRepo.lagreDialog(dialogId = UUID.randomUUID(), sykmeldingId = sykmeldingId) }
        }
    })
