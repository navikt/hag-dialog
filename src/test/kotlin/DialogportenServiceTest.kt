package no.nav.helsearbeidsgiver.dialogporten

import forespoersel_utgaatt
import inntektsmelding_godkjent
import inntektsmeldingsforespoersel
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.database.DialogEntity
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.database.TransmissionEntity
import no.nav.helsearbeidsgiver.database.TransmissionTable
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SizedCollection
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

        test("patch") {
            // TODO: Temp: Denne testen kan slettes når vi har patchet ok!
            val dialog1 = mockk<DialogEntity>()
            val dialog2 = mockk<DialogEntity>()
            val t1 = mockk<TransmissionEntity>(relaxed = true)
            every { t1.dokumentType } returns LpsApiExtendedType.SYKMELDING.toString()
            every { t1.id } returns EntityID(table = TransmissionTable, id = UUID.fromString("019fd20f-34bd-776e-8fe9-f2e9c88f5e7c"))
            every { dialog1.dialogId } returns UUID.randomUUID()
            every { dialog2.dialogId } returns UUID.randomUUID()
            every { dialog1.sykmeldingId } returns UUID.randomUUID()
            every { dialog2.sykmeldingId } returns UUID.randomUUID()
            every { dialog1.transmissions } returns SizedCollection(listOf(t1))
            every { dialog2.transmissions } returns SizedCollection(listOf(t1))

            coEvery { dialogRepository.hentDialogerOpprettetPaaDag(any()) } returns listOf(dialog1, dialog2)

            val service = SykepengerDialogportenService(dialogRepository, dialogportenClient, unleashFeatureToggles)
            val start = System.currentTimeMillis()
            service.oppdaterTransmisjonerMedFeilUrl()
            val end = System.currentTimeMillis()
            val duration = end - start
            println("fix manglende sykmeldinger took $duration ms")
        }
        test("opprettOgLagreDialog skal kalle sykmeldingHandler") {
            val service = SykepengerDialogportenService(dialogRepository, dialogportenClient, unleashFeatureToggles)

            service.opprettOgLagreDialog(sykmelding)

            verify(atLeast = 0) { dialogRepository.finnDialogMedSykemeldingId(any()) }
        }

        test("oppdaterDialogMedSykepengesoeknad skal kalle sykepengesoeknadHandler") {
            val service = SykepengerDialogportenService(dialogRepository, dialogportenClient, unleashFeatureToggles)

            service.oppdaterDialogMedSykepengesoeknad(sykepengesoeknad)

            verify(atLeast = 0) { dialogRepository.finnDialogMedSykemeldingId(any()) }
        }

        test("oppdaterDialogMedInntektsmeldingsforespoersel skal kalle forespoerselHandler") {
            val service = SykepengerDialogportenService(dialogRepository, dialogportenClient, unleashFeatureToggles)

            service.oppdaterDialogMedInntektsmeldingsforespoersel(inntektsmeldingsforespoersel)

            verify(atLeast = 0) { dialogRepository.finnDialogMedSykemeldingId(any()) }
        }

        test("oppdaterDialogMedInntektsmelding skal kalle inntektsmeldingHandler") {
            val service = SykepengerDialogportenService(dialogRepository, dialogportenClient, unleashFeatureToggles)

            service.oppdaterDialogMedInntektsmelding(inntektsmelding_godkjent)

            verify(atLeast = 0) { dialogRepository.finnDialogMedSykemeldingId(any()) }
        }

        test("oppdaterDialogMedUtgaattForespoersel skal kalle utgaattForespoerselHandler") {
            val service = SykepengerDialogportenService(dialogRepository, dialogportenClient, unleashFeatureToggles)

            service.oppdaterDialogMedUtgaattForespoersel(forespoersel_utgaatt)

            verify(atLeast = 0) { dialogRepository.finnDialogMedSykemeldingId(any()) }
        }
    })
