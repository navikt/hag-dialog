package dialogporten.handlers

import io.kotest.core.spec.style.FunSpec
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.Tjeneste
import no.nav.helsearbeidsgiver.database.DialogEntity
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.database.TransmissionEntity
import no.nav.helsearbeidsgiver.database.TransmissionTable
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.LpsApiExtendedType
import no.nav.helsearbeidsgiver.dialogporten.domene.TransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.handlers.SykepengesoeknadHandler
import no.nav.helsearbeidsgiver.notifikasjon.AgNotifikasjonService
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import org.jetbrains.exposed.dao.id.EntityID
import sykepengesoeknad
import java.util.UUID

class SykepengesoeknadHandlerTest :
    FunSpec({

        val dialogportenClientMock = mockk<DialogportenClient>()
        val dialogRepositoryMock = mockk<DialogRepository>()
        val agNotifikasjonServiceMock = mockk<AgNotifikasjonService>(relaxed = true)
        val unleashFeatureTogglesMock = mockk<UnleashFeatureToggles>()
        val sykepengeSoeknadhandler =
            SykepengesoeknadHandler(
                dialogRepositoryMock,
                dialogportenClientMock,
                agNotifikasjonServiceMock,
                unleashFeatureTogglesMock,
            )

        beforeTest {
            clearAllMocks()
            every { unleashFeatureTogglesMock.skalOppretteNotifikasjoner() } returns true
        }

        test("skal oppdatere dialog med sykepengesøknad") {
            val dialogId = UUID.randomUUID()
            val transmissionId = UUID.randomUUID()
            val dialogEntity =
                mockk<DialogEntity> {
                    every { this@mockk.dialogId } returns dialogId
                    every { transmissionByDokumentId(sykepengesoeknad.soeknadId) } returns null
                }

            every { dialogRepositoryMock.finnDialogMedSykemeldingId(sykepengesoeknad.sykmeldingId) } returns dialogEntity
            coEvery { dialogportenClientMock.addTransmission(any(), any<TransmissionRequest>()) } returns transmissionId
            coEvery { dialogportenClientMock.removeApiOnly(any()) } just Runs
            every { dialogRepositoryMock.oppdaterDialogMedTransmission(any(), any(), any(), any(), any()) } just Runs

            sykepengeSoeknadhandler.oppdaterDialog(sykepengesoeknad)

            verify(exactly = 1) { dialogRepositoryMock.finnDialogMedSykemeldingId(sykepengesoeknad.sykmeldingId) }
            coVerify(exactly = 1) { dialogportenClientMock.addTransmission(dialogId, any<TransmissionRequest>()) }
            verify(exactly = 1) {
                dialogRepositoryMock.oppdaterDialogMedTransmission(
                    sykmeldingId = sykepengesoeknad.sykmeldingId,
                    transmissionId = transmissionId,
                    dokumentId = sykepengesoeknad.soeknadId,
                    dokumentType = LpsApiExtendedType.SYKEPENGESOEKNAD.toString(),
                )
            }
        }

        test("skal ikke oppdatere dialog når dialog ikke finnes") {
            every { dialogRepositoryMock.finnDialogMedSykemeldingId(sykepengesoeknad.sykmeldingId) } returns null

            sykepengeSoeknadhandler.oppdaterDialog(sykepengesoeknad)

            verify(exactly = 1) { dialogRepositoryMock.finnDialogMedSykemeldingId(sykepengesoeknad.sykmeldingId) }
            coVerify(exactly = 0) { dialogportenClientMock.addTransmission(any(), any<TransmissionRequest>()) }
            verify(exactly = 0) { dialogRepositoryMock.oppdaterDialogMedTransmission(any(), any(), any(), any(), any()) }
            verify(exactly = 0) { agNotifikasjonServiceMock.opprettAgNotifikasjonsJobb(any(), any()) }
        }

        test("skal hoppe over transmission hvis den allerede finnes, men fortsatt legge notifikasjon i kø") {
            val dialogId = UUID.randomUUID()
            val eksisterendeTransmission = mockk<TransmissionEntity>()
            val dialogEntity =
                mockk<DialogEntity> {
                    every { this@mockk.dialogId } returns dialogId
                    every { transmissionByDokumentId(sykepengesoeknad.soeknadId) } returns eksisterendeTransmission
                }

            every { dialogRepositoryMock.finnDialogMedSykemeldingId(sykepengesoeknad.sykmeldingId) } returns dialogEntity

            sykepengeSoeknadhandler.oppdaterDialog(sykepengesoeknad)

            coVerify(exactly = 0) { dialogportenClientMock.addTransmission(any(), any<TransmissionRequest>()) }
            verify(exactly = 0) { dialogRepositoryMock.oppdaterDialogMedTransmission(any(), any(), any(), any(), any()) }
            verify(exactly = 1) {
                agNotifikasjonServiceMock.opprettAgNotifikasjonsJobb(
                    dokumentId = sykepengesoeknad.soeknadId,
                    tjeneste = Tjeneste.SOEKNAD,
                )
            }
        }

        test("skal legge notifikasjon i kø etter oppdatering av dialog") {
            val dialogId = UUID.randomUUID()
            val transmissionId = UUID.randomUUID()
            val dialogEntity =
                mockk<DialogEntity> {
                    every { this@mockk.dialogId } returns dialogId
                    every { transmissionByDokumentId(sykepengesoeknad.soeknadId) } returns null
                }

            every { dialogRepositoryMock.finnDialogMedSykemeldingId(sykepengesoeknad.sykmeldingId) } returns dialogEntity
            coEvery { dialogportenClientMock.addTransmission(any(), any<TransmissionRequest>()) } returns transmissionId
            coEvery { dialogportenClientMock.removeApiOnly(any()) } just Runs
            every { dialogRepositoryMock.oppdaterDialogMedTransmission(any(), any(), any(), any(), any()) } just Runs

            sykepengeSoeknadhandler.oppdaterDialog(sykepengesoeknad)

            coVerify(exactly = 1) { dialogportenClientMock.addTransmission(dialogId, any<TransmissionRequest>()) }
            verify(exactly = 1) {
                agNotifikasjonServiceMock.opprettAgNotifikasjonsJobb(
                    dokumentId = sykepengesoeknad.soeknadId,
                    tjeneste = Tjeneste.SOEKNAD,
                )
            }
        }

        test("skal opprette korrigert transmission med relatedTransmissionId soeknad er korrigert") {
            val dialogId = UUID.randomUUID()
            val transmissionId = UUID.randomUUID()
            val korrigererSoeknadId = UUID.randomUUID()
            val korrigertTransmissionId = UUID.randomUUID()
            val dialogEntity =
                mockk<DialogEntity> {
                    every { this@mockk.dialogId } returns dialogId
                    every { transmissionByDokumentId(any()) } returns null
                }
            val korrigertTransmission =
                mockk<TransmissionEntity> {
                    every { id } returns EntityID(korrigertTransmissionId, TransmissionTable)
                }

            every { dialogRepositoryMock.finnDialogMedSykemeldingId(sykepengesoeknad.sykmeldingId) } returns dialogEntity
            every { dialogRepositoryMock.hentTransmissionMedDokumentId(korrigererSoeknadId) } returns korrigertTransmission
            coEvery { dialogportenClientMock.removeApiOnly(any()) } just Runs
            coEvery { dialogportenClientMock.addTransmission(any(), any<TransmissionRequest>()) } returns transmissionId
            every { dialogRepositoryMock.oppdaterDialogMedTransmission(any(), any(), any(), any(), any()) } just Runs

            sykepengeSoeknadhandler.oppdaterDialog(sykepengesoeknad.copy(korrigerer = korrigererSoeknadId))

            verify(exactly = 1) { dialogRepositoryMock.hentTransmissionMedDokumentId(korrigererSoeknadId) }
            coVerify(exactly = 1) { dialogportenClientMock.addTransmission(dialogId, any<TransmissionRequest>()) }
            verify(exactly = 1) {
                dialogRepositoryMock.oppdaterDialogMedTransmission(
                    sykmeldingId = sykepengesoeknad.sykmeldingId,
                    transmissionId = transmissionId,
                    dokumentId = sykepengesoeknad.soeknadId,
                    dokumentType = LpsApiExtendedType.SYKEPENGESOEKNAD.toString(),
                    relatedTransmissionId = korrigertTransmissionId,
                )
            }
        }
    })
