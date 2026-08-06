package no.nav.helsearbeidsgiver.dialogporten

import forespoersel_utgaatt
import inntektsmelding_godkjent
import inntektsmeldingsforespoersel
import io.kotest.core.spec.style.FunSpec
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.helsearbeidsgiver.database.DialogEntity
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.dialogporten.domene.Content
import no.nav.helsearbeidsgiver.dialogporten.domene.DialogResponse
import no.nav.helsearbeidsgiver.dialogporten.domene.DialogStatus
import no.nav.helsearbeidsgiver.dialogporten.domene.Transmission
import no.nav.helsearbeidsgiver.dialogporten.domene.Transmission.TransmissionType
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import sykepengesoeknad
import sykmelding
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

class DialogportenServiceTest :
    FunSpec({
        beforeTest {
            clearAllMocks()
        }
        val dialogRepository = mockk<DialogRepository>(relaxed = true)
        val dialogportenClient = mockk<DialogportenClient>(relaxed = true)
        val unleashFeatureToggles = mockk<UnleashFeatureToggles>(relaxed = true)
        val agNotifikasjonKlient = mockk<ArbeidsgiverNotifikasjonKlient>(relaxed = true)

        test("fix sykmelding") {
            // TODO: Temp: Denne testen kan slettes når vi har patchet ok!
            val dialog1 = mockk<DialogEntity>()
            val dialog2 = mockk<DialogEntity>()
            every { dialog1.dialogId } returns UUID.randomUUID()
            every { dialog2.dialogId } returns UUID.randomUUID()
            every { dialog1.sykmeldingId } returns UUID.randomUUID()
            every { dialog2.sykmeldingId } returns UUID.randomUUID()
            coEvery { dialogRepository.hentDialogerOpprettetPaaDag(any()) } returns listOf(dialog1, dialog2)

            coEvery { dialogportenClient.getDialog(any()) } coAnswers {
                delay(10.milliseconds)
                Result.success(
                    DialogResponse(
                        id = "1",
                        serviceResource = "2",
                        party = "3",
                        externalReference = "4",
                        idempotentKey = "5",
                        status = DialogStatus.Completed,
                        content = mockk<Content>(),
                        transmissions =
                            listOf(
                                Transmission(
                                    id = UUID.fromString("019fd20f-34bd-776e-8fe9-f2e9c88f5e7c"),
                                    extendedType = LpsApiExtendedType.SYKMELDING.toString(),
                                    type = TransmissionType.Information,
                                    sender = Transmission.Sender("sfds"),
                                    content = mockk<Content>(),
                                    externalReference = "1",
                                ),
                            ),
                    ),
                )
            }

            coEvery { dialogRepository.oppdaterDialogMedTransmission(any(), any(), any(), any(), any()) } just Runs

            val service = SykepengerDialogportenService(dialogRepository, dialogportenClient, unleashFeatureToggles, agNotifikasjonKlient)
            val start = System.currentTimeMillis()
            service.fixManglendeSykmeldinger()
            val end = System.currentTimeMillis()
            val duration = end - start
            println("fix manglende sykmeldinger took $duration ms")
        }
        test("opprettOgLagreDialog skal kalle sykmeldingHandler") {
            val service = SykepengerDialogportenService(dialogRepository, dialogportenClient, unleashFeatureToggles, agNotifikasjonKlient)

            service.opprettOgLagreDialog(sykmelding)

            verify(atLeast = 0) { dialogRepository.finnDialogMedSykemeldingId(any()) }
        }

        test("oppdaterDialogMedSykepengesoeknad skal kalle sykepengesoeknadHandler") {
            val service = SykepengerDialogportenService(dialogRepository, dialogportenClient, unleashFeatureToggles, agNotifikasjonKlient)

            service.oppdaterDialogMedSykepengesoeknad(sykepengesoeknad)

            verify(atLeast = 0) { dialogRepository.finnDialogMedSykemeldingId(any()) }
        }

        test("oppdaterDialogMedInntektsmeldingsforespoersel skal kalle forespoerselHandler") {
            val service = SykepengerDialogportenService(dialogRepository, dialogportenClient, unleashFeatureToggles, agNotifikasjonKlient)

            service.oppdaterDialogMedInntektsmeldingsforespoersel(inntektsmeldingsforespoersel)

            verify(atLeast = 0) { dialogRepository.finnDialogMedSykemeldingId(any()) }
        }

        test("oppdaterDialogMedInntektsmelding skal kalle inntektsmeldingHandler") {
            val service = SykepengerDialogportenService(dialogRepository, dialogportenClient, unleashFeatureToggles, agNotifikasjonKlient)

            service.oppdaterDialogMedInntektsmelding(inntektsmelding_godkjent)

            verify(atLeast = 0) { dialogRepository.finnDialogMedSykemeldingId(any()) }
        }

        test("oppdaterDialogMedUtgaattForespoersel skal kalle utgaattForespoerselHandler") {
            val service = SykepengerDialogportenService(dialogRepository, dialogportenClient, unleashFeatureToggles, agNotifikasjonKlient)

            service.oppdaterDialogMedUtgaattForespoersel(forespoersel_utgaatt)

            verify(atLeast = 0) { dialogRepository.finnDialogMedSykemeldingId(any()) }
        }
    })
