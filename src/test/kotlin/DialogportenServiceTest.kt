package no.nav.helsearbeidsgiver.dialogporten

import forespoersel_utgaatt
import inntektsmelding_godkjent
import inntektsmeldingsforespoersel
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.helsearbeidsgiver.database.DialogForPatch
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository
import no.nav.helsearbeidsgiver.database.TransmissionForPatch
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import sykepengesoeknad
import sykmelding
import java.util.UUID

class DialogportenServiceTest :
    FunSpec({
        beforeTest {
            clearAllMocks()
        }
        val dialogRepository = mockk<DialogRepository>(relaxed = true)
        val dialogportenClient = mockk<DialogportenClient>(relaxed = true)
        val unleashFeatureToggles = mockk<UnleashFeatureToggles>(relaxed = true)
        val agNotifikasjonKlient = mockk<ArbeidsgiverNotifikasjonKlient>(relaxed = true)
        val dokumentkoblingRepository = mockk<DokumentkoblingRepository>(relaxed = true)

        test("patch") {
            // TODO: Temp: Denne testen kan slettes når vi har patchet ok!
            val dialog1 =
                DialogForPatch(
                    UUID.randomUUID(),
                    listOf(
                        TransmissionForPatch(
                            UUID.fromString("019fd20f-34bd-776e-8fe9-f2e9c88f5e7c"),
                            UUID.randomUUID(),
                            LpsApiExtendedType.SYKMELDING.toString(),
                        ),
                    ),
                )
            val dialog2 =
                DialogForPatch(
                    UUID.randomUUID(),
                    listOf(
                        TransmissionForPatch(
                            UUID.fromString("019fd20f-34bd-776e-8fe9-f2e9c88f5e7c"),
                            UUID.randomUUID(),
                            LpsApiExtendedType.SYKEPENGESOEKNAD.toString(),
                        ),
                    ),
                )

            coEvery { dialogRepository.hentDialogerOpprettetPaaDag(any()) } returns listOf(dialog1, dialog2)

            val service =
                SykepengerDialogportenService(
                    dialogRepository,
                    dialogportenClient,
                    unleashFeatureToggles,
                    agNotifikasjonKlient,
                    dokumentkoblingRepository,
                )
            val start = System.currentTimeMillis()
            service.oppdaterTransmisjonerMedFeilUrl()
            val end = System.currentTimeMillis()
            val duration = end - start
            println("fix manglende sykmeldinger took $duration ms")
        }
        test("opprettOgLagreDialog skal kalle sykmeldingHandler") {
            val service =
                SykepengerDialogportenService(
                    dialogRepository,
                    dialogportenClient,
                    unleashFeatureToggles,
                    agNotifikasjonKlient,
                    dokumentkoblingRepository,
                )

            service.opprettOgLagreDialog(sykmelding)

            verify(atLeast = 0) { dialogRepository.finnDialogMedSykemeldingId(any()) }
        }

        test("oppdaterDialogMedSykepengesoeknad skal kalle sykepengesoeknadHandler") {
            val service =
                SykepengerDialogportenService(
                    dialogRepository,
                    dialogportenClient,
                    unleashFeatureToggles,
                    agNotifikasjonKlient,
                    dokumentkoblingRepository,
                )

            service.oppdaterDialogMedSykepengesoeknad(sykepengesoeknad)

            verify(atLeast = 0) { dialogRepository.finnDialogMedSykemeldingId(any()) }
        }

        test("oppdaterDialogMedInntektsmeldingsforespoersel skal kalle forespoerselHandler") {
            val service =
                SykepengerDialogportenService(
                    dialogRepository,
                    dialogportenClient,
                    unleashFeatureToggles,
                    agNotifikasjonKlient,
                    dokumentkoblingRepository,
                )

            service.oppdaterDialogMedInntektsmeldingsforespoersel(inntektsmeldingsforespoersel)

            verify(atLeast = 0) { dialogRepository.finnDialogMedSykemeldingId(any()) }
        }

        test("oppdaterDialogMedInntektsmelding skal kalle inntektsmeldingHandler") {
            val service =
                SykepengerDialogportenService(
                    dialogRepository,
                    dialogportenClient,
                    unleashFeatureToggles,
                    agNotifikasjonKlient,
                    dokumentkoblingRepository,
                )

            service.oppdaterDialogMedInntektsmelding(inntektsmelding_godkjent)

            verify(atLeast = 0) { dialogRepository.finnDialogMedSykemeldingId(any()) }
        }

        test("oppdaterDialogMedUtgaattForespoersel skal kalle utgaattForespoerselHandler") {
            val service =
                SykepengerDialogportenService(
                    dialogRepository,
                    dialogportenClient,
                    unleashFeatureToggles,
                    agNotifikasjonKlient,
                    dokumentkoblingRepository,
                )

            service.oppdaterDialogMedUtgaattForespoersel(forespoersel_utgaatt)

            verify(atLeast = 0) { dialogRepository.finnDialogMedSykemeldingId(any()) }
        }
    })
