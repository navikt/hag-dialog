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

        test("oppretter dialog med sykmelding og lagrer dialogId i databasen") {
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
                "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/sykmelding/${sykmelding.sykmeldingId}"
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

        test("oppdaterer dialog med sykepengesøknad") {
            val dialogId = UUID.randomUUID()

            every { dialogRepositoryMock.finnDialogId(any()) } returns dialogId

            coEvery {
                dialogportenClientMock.oppdaterDialogMedSykepengesoeknad(
                    any(),
                    any(),
                )
            } just Runs

            dialogportenService.oppdaterDialogMedSykepengesoeknad(sykepengesoeknad)

            verify(exactly = 1) { dialogRepositoryMock.finnDialogId(sykepengesoeknad.sykmeldingId) }

            val forventetUrl = "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/sykepengesoeknad/${sykepengesoeknad.soeknadId}"
            coVerify(exactly = 1) {
                dialogportenClientMock.oppdaterDialogMedSykepengesoeknad(
                    dialogId,
                    forventetUrl,
                )
            }
        }

        test("oppdaterer dialog med inntektsmeldingsforespørsel") {
            val dialogId = UUID.randomUUID()

            every { dialogRepositoryMock.finnDialogId(any()) } returns dialogId

            coEvery {
                dialogportenClientMock.oppdaterDialogMedInntektsmeldingsforespoersel(
                    any(),
                    any(),
                    any(),
                )
            } just Runs

            dialogportenService.oppdaterDialogMedInntektsmeldingsforespoersel(inntektsmeldingsforespoersel)

            verify(exactly = 1) { dialogRepositoryMock.finnDialogId(inntektsmeldingsforespoersel.sykmeldingId) }

            val forventetForespoerselUrl =
                "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/forespoersel/${inntektsmeldingsforespoersel.forespoerselId}"
            val forventetDokumentasjonUrl = "${Env.Nav.arbeidsgiverApiBaseUrl}/swagger"

            coVerify(exactly = 1) {
                dialogportenClientMock.oppdaterDialogMedInntektsmeldingsforespoersel(
                    dialogId,
                    forventetForespoerselUrl,
                    forventetDokumentasjonUrl,
                )
            }
        }

        test("ignorerer sykepengesøknad dersom vi ikke finner tilhørende dialog i databasen") {
            every { dialogRepositoryMock.finnDialogId(any()) } returns null

            dialogportenService.oppdaterDialogMedSykepengesoeknad(sykepengesoeknad)

            verify(exactly = 1) { dialogRepositoryMock.finnDialogId(sykepengesoeknad.sykmeldingId) }

            coVerify(exactly = 0) {
                dialogportenClientMock.oppdaterDialogMedSykepengesoeknad(
                    any(),
                    any(),
                )
            }
        }

        test("ignorerer inntektsmeldingforespørsel dersom vi ikke finner tilhørende dialog i databasen") {
            every { dialogRepositoryMock.finnDialogId(any()) } returns null

            dialogportenService.oppdaterDialogMedInntektsmeldingsforespoersel(inntektsmeldingsforespoersel)

            verify(exactly = 1) { dialogRepositoryMock.finnDialogId(inntektsmeldingsforespoersel.sykmeldingId) }

            coVerify(exactly = 0) {
                dialogportenClientMock.oppdaterDialogMedInntektsmeldingsforespoersel(
                    any(),
                    any(),
                    any(),
                )
            }
        }
    })
