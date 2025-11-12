import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.database.DialogEntity
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.database.DialogTable
import no.nav.helsearbeidsgiver.database.TransmissionEntity
import no.nav.helsearbeidsgiver.database.TransmissionTable
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class DialogRepositoryTest :
    FunSpecWithDb(listOf(TransmissionTable, DialogTable), { db ->
        val repository = DialogRepository(db)

        test("lagreDialog skal legge til dialog i databasen") {
            val dialogId = UUID.randomUUID()
            val sykmeldingId = UUID.randomUUID()

            repository.lagreDialog(dialogId, sykmeldingId)

            transaction(db) {
                val dialog = DialogEntity.findById(dialogId)
                dialog.shouldNotBeNull()
                dialog.id.value shouldBe dialogId
                dialog.sykmeldingId shouldBe sykmeldingId
            }
        }

        test("lagreDialog skal kaste exception ved duplikat sykmeldingId") {
            val dialogId = UUID.randomUUID()
            val sykmeldingId = UUID.randomUUID()

            repository.lagreDialog(dialogId, sykmeldingId)

            shouldThrow<ExposedSQLException> {
                repository.lagreDialog(dialogId, UUID.randomUUID())
            }
        }

        test("finnDialogIdMedSykemeldingId skal returnere dialog når den finnes") {
            val dialogId = UUID.randomUUID()
            val sykmeldingId = UUID.randomUUID()

            repository.lagreDialog(dialogId, sykmeldingId)

            val result = repository.finnDialogMedSykemeldingId(sykmeldingId)
            result.shouldNotBeNull()
            result.id.value shouldBe dialogId
            result.sykmeldingId shouldBe sykmeldingId
        }

        test("finnDialogIdMedSykemeldingId skal returnere null når dialog ikke finnes") {
            val sykmeldingId = UUID.randomUUID()

            val result = repository.finnDialogMedSykemeldingId(sykmeldingId)
            result.shouldBeNull()
        }

        test("oppdaterDialogMedTransmission skal legge til transmission i databasen") {
            val dialogId = UUID.randomUUID()
            val sykmeldingId = UUID.randomUUID()
            val transmissionId = UUID.randomUUID()
            val dokumentId = UUID.randomUUID()
            val dokumentType = "INNTEKTSMELDING"

            repository.lagreDialog(dialogId, sykmeldingId)
            repository.oppdaterDialogMedTransmission(sykmeldingId, transmissionId, dokumentId, dokumentType)

            transaction(db) {
                val transmission = TransmissionEntity.findById(transmissionId)
                transmission.shouldNotBeNull()
                transmission.dialog.id.value shouldBe dialogId
                transmission.dokumentId shouldBe dokumentId
                transmission.dokumentType shouldBe dokumentType
                transmission.relatedTransmission.shouldBeNull()
            }
        }

        test("oppdaterDialogMedTransmission skal legge til transmission med relatedTransmission") {
            val dialogId = UUID.randomUUID()
            val sykmeldingId = UUID.randomUUID()
            val transmissionId1 = UUID.randomUUID()
            val transmissionId2 = UUID.randomUUID()
            val dokumentId1 = UUID.randomUUID()
            val dokumentId2 = UUID.randomUUID()

            repository.lagreDialog(dialogId, sykmeldingId)
            repository.oppdaterDialogMedTransmission(sykmeldingId, transmissionId1, dokumentId1, "TYPE1")
            repository.oppdaterDialogMedTransmission(sykmeldingId, transmissionId2, dokumentId2, "TYPE2", transmissionId1)

            transaction(db) {
                val transmission = TransmissionEntity.findById(transmissionId2)
                transmission.shouldNotBeNull()
                transmission.relatedTransmission shouldBe transmissionId1
            }
        }

        test("oppdaterDialogMedTransmission skal kaste exception når dialog ikke finnes") {
            val sykmeldingId = UUID.randomUUID()
            val transmissionId = UUID.randomUUID()
            val dokumentId = UUID.randomUUID()

            shouldThrow<IllegalArgumentException> {
                repository.oppdaterDialogMedTransmission(sykmeldingId, transmissionId, dokumentId, "TYPE")
            }
        }

        test("dialog kan ha flere transmissions") {
            val dialogId = UUID.randomUUID()
            val sykmeldingId = UUID.randomUUID()
            val transmissionId1 = UUID.randomUUID()
            val transmissionId2 = UUID.randomUUID()

            repository.lagreDialog(dialogId, sykmeldingId)
            repository.oppdaterDialogMedTransmission(sykmeldingId, transmissionId1, UUID.randomUUID(), "TYPE1")
            repository.oppdaterDialogMedTransmission(sykmeldingId, transmissionId2, UUID.randomUUID(), "TYPE2")

            transaction(db) {
                val dialog = DialogEntity.findById(dialogId)
                dialog.shouldNotBeNull()
                dialog.transmissions shouldHaveSize 2
            }
        }
    })
