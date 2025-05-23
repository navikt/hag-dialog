import io.kotest.matchers.collections.shouldBeEmpty
import no.nav.helsearbeidsgiver.DialogEntitet
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

        //val inntektsmeldingRepo = DialogRepository(db)
        val testRepo = TestRepo(db)

        test("skal lagre inntektsmeldingskjema og inntektsmelding") {
            transaction {
                DialogEntitet.selectAll().toList()
            }.shouldBeEmpty()
        }
    })
