import io.kotest.core.spec.style.FunSpec
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.DialogRepository
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.dialogporten.SykmeldingTransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.ApiAction
import no.nav.helsearbeidsgiver.dialogporten.domene.CreateDialogRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.Transmission
import no.nav.helsearbeidsgiver.dialogporten.domene.lagTransmissionMedVedlegg
import no.nav.helsearbeidsgiver.dialogporten.getSykmeldingsPerioderString
import no.nav.helsearbeidsgiver.utils.tilNorskFormat
import java.util.UUID

class DialogportenServiceTest :
    FunSpec({

        beforeTest {
            clearAllMocks()
        }

        val dialogportenClientMock = mockk<DialogportenClient>()
        val dialogRepositoryMock = mockk<DialogRepository>()

        val dialogportenService = DialogportenService(dialogRepositoryMock, dialogportenClientMock)

        test("oppretter dialog med sykmelding og lagrer dialogId i databasen") {
            val dialogId = UUID.randomUUID()
            coEvery {
                dialogportenClientMock.createDialog(
                    any(),
                )
            } returns dialogId

            every { dialogRepositoryMock.lagreDialog(any(), any()) } just Runs

            dialogportenService.opprettOgLagreDialog(sykmelding)

            val forventetUrl =
                "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/sykmelding/${sykmelding.sykmeldingId}"

            coVerify(exactly = 1) {
                dialogportenClientMock.createDialog(
                    CreateDialogRequest(
                        orgnr = orgnr,
                        externalReference = sykmelding.sykmeldingId.toString(),
                        title =
                            "Sykepenger for ${sykmelding.fulltNavn} (f. ${sykmelding.foedselsdato.tilNorskFormat()})",
                        summary =
                            sykmelding.sykmeldingsperioder
                                .getSykmeldingsPerioderString(),
                        transmissions =
                            listOf(
                                lagTransmissionMedVedlegg(
                                    transmissionRequest = SykmeldingTransmissionRequest(sykmelding),
                                ),
                            ),
                        isApiOnly = true,
                    ),
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
                dialogportenClientMock.addTransmission(
                    any(),
                    any(),
                )
            } returns UUID.randomUUID()

            dialogportenService.oppdaterDialogMedSykepengesoeknad(sykepengesoeknad)

            verify(exactly = 1) { dialogRepositoryMock.finnDialogId(sykepengesoeknad.sykmeldingId) }

            coVerify(exactly = 1) {
                dialogportenClientMock.addTransmission(
                    dialogId,
                    any<Transmission>(),
                )
            }
        }

        test("oppdaterer dialog med inntektsmeldingsforespørsel") {
            val dialogId = UUID.randomUUID()

            every { dialogRepositoryMock.finnDialogId(any()) } returns dialogId

            coEvery {
                dialogportenClientMock.addTransmission(
                    any(),
                    any(),
                )
            } returns UUID.randomUUID()
            coEvery {
                dialogportenClientMock.addAction(any(), any())
            } just Runs

            dialogportenService.oppdaterDialogMedInntektsmeldingsforespoersel(inntektsmeldingsforespoersel)

            verify(exactly = 1) { dialogRepositoryMock.finnDialogId(inntektsmeldingsforespoersel.sykmeldingId) }

            coVerifySequence {
                dialogportenClientMock.addTransmission(
                    dialogId,
                    any<Transmission>(),
                )
                dialogportenClientMock.addAction(dialogId, any<ApiAction>())
            }
        }

        test("ignorerer sykepengesøknad dersom vi ikke finner tilhørende dialog i databasen") {
            every { dialogRepositoryMock.finnDialogId(any()) } returns null

            dialogportenService.oppdaterDialogMedSykepengesoeknad(sykepengesoeknad)

            verify(exactly = 1) { dialogRepositoryMock.finnDialogId(sykepengesoeknad.sykmeldingId) }

            coVerify(exactly = 0) {
                dialogportenClientMock.addTransmission(
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
                dialogportenClientMock.addTransmission(
                    any(),
                    any(),
                )
            }
        }
    })
