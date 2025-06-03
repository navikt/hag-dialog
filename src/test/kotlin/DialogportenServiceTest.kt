import io.kotest.core.spec.style.FunSpec
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.DialogRepository
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.utils.json.toJson
import java.util.UUID

class DialogportenServiceTest :
    FunSpec({

        beforeTest {
            clearAllMocks()
        }

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
            } returns dialogId.toJson().toString()

            every { dialogRepositoryMock.lagreDialog(any(), any()) } just Runs

            dialogportenService.opprettOgLagreDialog(sykmelding)

            val forventetUrl =
                "${Env.Nav.arbeidsgiverApiBaseUrl}/sykmelding/${sykmelding.sykmeldingId}"
            coVerify(exactly = 1) {
                dialogportenClientMock.opprettDialogMedSykmelding(
                    orgnr = sykmelding.orgnr.toString(),
                    dialogTittel = any(),
                    dialogSammendrag = any(),
                    sykmeldingId = sykepengesoeknad.sykmeldingId,
                    sykmeldingJsonUrl = forventetUrl,
                )
            }
            verify(exactly = 1) {
                dialogRepositoryMock.lagreDialog(
                    dialogId = dialogId,
                    sykmeldingId = sykepengesoeknad.sykmeldingId,
                )
            }
        }

        test("behandler sykepengesøknad") {
            val dialogId = UUID.randomUUID()

            every { dialogRepositoryMock.finnDialogId(any()) } returns dialogId

            coEvery {
                dialogportenClientMock.oppdaterDialogMedSykepengesoeknad(
                    any(),
                    any(),
                )
            } just Runs

            dialogportenService.oppdaterDialog(sykepengesoeknad)

            verify(exactly = 1) { dialogRepositoryMock.finnDialogId(sykepengesoeknad.sykmeldingId) }

            val forventetUrl = "${Env.Nav.arbeidsgiverApiBaseUrl}/sykepengesoeknad/${sykepengesoeknad.soeknadId}"
            coVerify(exactly = 1) {
                dialogportenClientMock.oppdaterDialogMedSykepengesoeknad(
                    dialogId,
                    forventetUrl,
                )
            }
        }

        test("behandler ignorerer sykepengesøknad dersom vi ikke finner tilhørende dialog i databasen") {
            every { dialogRepositoryMock.finnDialogId(any()) } returns null

            dialogportenService.oppdaterDialog(sykepengesoeknad)

            verify(exactly = 1) { dialogRepositoryMock.finnDialogId(sykepengesoeknad.sykmeldingId) }

            coVerify(exactly = 0) {
                dialogportenClientMock.oppdaterDialogMedSykepengesoeknad(
                    any(),
                    any(),
                )
            }
        }
    })
