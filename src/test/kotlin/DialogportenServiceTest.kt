import io.kotest.core.spec.style.FunSpec
import io.ktor.http.ContentType
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
import no.nav.helsearbeidsgiver.dialogporten.DialogportenKlient
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.dialogporten.domene.Content
import no.nav.helsearbeidsgiver.dialogporten.domene.CreateDialogRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.DialogStatus
import no.nav.helsearbeidsgiver.dialogporten.domene.Transmission
import no.nav.helsearbeidsgiver.dialogporten.domene.lagContentValue
import no.nav.helsearbeidsgiver.dialogporten.getSykmeldingsPerioderString
import no.nav.helsearbeidsgiver.dialogporten.lagVedleggTransmission
import no.nav.helsearbeidsgiver.utils.tilNorskFormat
import java.util.UUID

class DialogportenServiceTest :
    FunSpec({

        beforeTest {
            clearAllMocks()
        }

        val dialogportenClientMock = mockk<DialogportenClient>()
        val dialogRepositoryMock = mockk<DialogRepository>()
        val dialogportenKlientMock = mockk<DialogportenKlient>()
        val ressurs = "testressurs"
        val dialogportenService = DialogportenService(dialogportenClientMock, dialogRepositoryMock, dialogportenKlientMock, ressurs)

        test("oppretter dialog med sykmelding og lagrer dialogId i databasen") {
            val dialogId = UUID.randomUUID()
            coEvery {
                dialogportenKlientMock.createDialog(
                    any(),
                )
            } returns dialogId

            every { dialogRepositoryMock.lagreDialog(any(), any()) } just Runs

            dialogportenService.opprettOgLagreDialog(sykmelding)

            val forventetUrl =
                "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/sykmelding/${sykmelding.sykmeldingId}"
            coVerify(exactly = 1) {
                dialogportenKlientMock.createDialog(
                    CreateDialogRequest(
                        serviceResource = "urn:altinn:resource:$ressurs",
                        party = "urn:altinn:organization:identifier-no:${sykmelding.orgnr}",
                        externalRefererence = sykmelding.sykmeldingId.toString(),
                        status = DialogStatus.New,
                        content =
                            Content(
                                title =
                                    "Sykepenger for ${sykmelding.fulltNavn} (f. ${sykmelding.foedselsdato.tilNorskFormat()})"
                                        .lagContentValue(),
                                summary =
                                    sykmelding.sykmeldingsperioder
                                        .getSykmeldingsPerioderString()
                                        .lagContentValue(),
                            ),
                        transmissions =
                            listOf(
                                lagVedleggTransmission(
                                    transmissionTittel = "Sykmelding",
                                    vedleggType = Transmission.ExtendedType.SYKMELDING,
                                    vedleggNavn = "Sykmelding.json",
                                    vedleggUrl = forventetUrl,
                                    vedleggMediaType = ContentType.Application.Json.contentType,
                                    vedleggConsumerType = Transmission.AttachmentUrlConsumerType.Api,
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
                dialogportenKlientMock.addTransmission(
                    any(),
                    any(),
                )
            } returns UUID.randomUUID()

            dialogportenService.oppdaterDialogMedSykepengesoeknad(sykepengesoeknad)

            verify(exactly = 1) { dialogRepositoryMock.finnDialogId(sykepengesoeknad.sykmeldingId) }

            val forventetUrl = "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/sykepengesoeknad/${sykepengesoeknad.soeknadId}"
            coVerify(exactly = 1) {
                dialogportenKlientMock.addTransmission(
                    dialogId,
                    any(),
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
