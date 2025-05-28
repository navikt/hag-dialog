import io.kotest.core.spec.style.FunSpec
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.DialogRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import java.util.UUID

class DialogportenServiceTest :
    FunSpec({
        val dialogportenClientMock = mockk<DialogportenClient>()
        val dialogRepositoryMock = mockk<DialogRepository>()
        val dialogportenService = DialogportenService(dialogportenClientMock, dialogRepositoryMock)

        test("behandler sykmelding") {
            val dialogId = UUID.randomUUID()
            coEvery {
                dialogportenClientMock.opprettDialogMedSykmelding(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns dialogId.toString()

            every { dialogRepositoryMock.lagreDialog(any(), any()) } just Runs

            dialogportenService.behandleSykmelding(sykmelding)

            coVerify(exactly = 1) {
                dialogportenClientMock.opprettDialogMedSykmelding(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            }
            verify(exactly = 1) { dialogRepositoryMock.lagreDialog(any(), any()) }
        }
    })
