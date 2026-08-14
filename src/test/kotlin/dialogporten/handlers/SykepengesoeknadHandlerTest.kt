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
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.helsearbeidsgiver.database.DialogEntity
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository
import no.nav.helsearbeidsgiver.database.SykmeldingEntity
import no.nav.helsearbeidsgiver.database.TransmissionEntity
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.LpsApiExtendedType
import no.nav.helsearbeidsgiver.dialogporten.domene.TransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.handlers.SykepengesoeknadHandler
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import sykepengesoeknad
import java.time.LocalDate
import java.util.UUID

class SykepengesoeknadHandlerTest :
    FunSpec({

        val dialogportenClientMock = mockk<DialogportenClient>()
        val dialogRepositoryMock = mockk<DialogRepository>()
        val unleashFeatureTogglesMock = mockk<UnleashFeatureToggles>()
        val agNotifikasjonKlientMock = mockk<ArbeidsgiverNotifikasjonKlient>()
        val dokumentkoblingRepositoryMock = mockk<DokumentkoblingRepository>()
        val sykepengeSoeknadhandler =
            SykepengesoeknadHandler(
                dialogRepositoryMock,
                dialogportenClientMock,
                unleashFeatureTogglesMock,
                agNotifikasjonKlientMock,
                dokumentkoblingRepositoryMock,
            )

        beforeTest {
            clearAllMocks()
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
            every { unleashFeatureTogglesMock.skalOppretteNotifikasjoner() } returns false

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
        }

        test("skal hoppe over transmission hvis den allerede finnes, men fortsatt opprette notifikasjoner") {
            val dialogId = UUID.randomUUID()
            val eksisterendeTransmission = mockk<TransmissionEntity>()
            val sykmeldingEntitet =
                mockk<SykmeldingEntity> {
                    every { data } returns
                        dokumentkobling.Sykmelding(
                            sykmeldingId = sykepengesoeknad.sykmeldingId,
                            orgnr = sykepengesoeknad.orgnr,
                            foedselsdato = LocalDate.of(1990, 1, 1),
                            fulltNavn = "OLA NORDMANN",
                            sykmeldingsperioder = emptyList(),
                        )
                }
            val dialogEntity =
                mockk<DialogEntity> {
                    every { this@mockk.dialogId } returns dialogId
                    every { transmissionByDokumentId(sykepengesoeknad.soeknadId) } returns eksisterendeTransmission
                }

            every { dialogRepositoryMock.finnDialogMedSykemeldingId(sykepengesoeknad.sykmeldingId) } returns dialogEntity
            every { unleashFeatureTogglesMock.skalOppretteNotifikasjoner() } returns true
            every { dokumentkoblingRepositoryMock.hentSykmeldingEntitet(sykepengesoeknad.sykmeldingId) } returns sykmeldingEntitet
            coEvery { agNotifikasjonKlientMock.opprettNySak(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
                UUID.randomUUID().toString()
            coEvery {
                agNotifikasjonKlientMock.opprettNyBeskjed(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns UUID.randomUUID().toString()

            sykepengeSoeknadhandler.oppdaterDialog(sykepengesoeknad)

            coVerify(exactly = 0) { dialogportenClientMock.addTransmission(any(), any<TransmissionRequest>()) }
            verify(exactly = 0) { dialogRepositoryMock.oppdaterDialogMedTransmission(any(), any(), any(), any(), any()) }
            coVerify(exactly = 1) { agNotifikasjonKlientMock.opprettNySak(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
            coVerify(exactly = 1) {
                agNotifikasjonKlientMock.opprettNyBeskjed(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
            }
        }

        test("skal opprette notifikasjoner etter oppdatering av dialog") {
            val dialogId = UUID.randomUUID()
            val transmissionId = UUID.randomUUID()
            val sykmeldingEntitet =
                mockk<SykmeldingEntity> {
                    every { data } returns
                        dokumentkobling.Sykmelding(
                            sykmeldingId = sykepengesoeknad.sykmeldingId,
                            orgnr = sykepengesoeknad.orgnr,
                            foedselsdato = LocalDate.of(1990, 1, 1),
                            fulltNavn = "OLA NORDMANN",
                            sykmeldingsperioder = emptyList(),
                        )
                }
            val dialogEntity =
                mockk<DialogEntity> {
                    every { this@mockk.dialogId } returns dialogId
                    every { transmissionByDokumentId(sykepengesoeknad.soeknadId) } returns null
                }

            every { dialogRepositoryMock.finnDialogMedSykemeldingId(sykepengesoeknad.sykmeldingId) } returns dialogEntity
            coEvery { dialogportenClientMock.addTransmission(any(), any<TransmissionRequest>()) } returns transmissionId
            coEvery { dialogportenClientMock.removeApiOnly(any()) } just Runs
            every { dialogRepositoryMock.oppdaterDialogMedTransmission(any(), any(), any(), any(), any()) } just Runs
            every { unleashFeatureTogglesMock.skalOppretteNotifikasjoner() } returns true
            every { dokumentkoblingRepositoryMock.hentSykmeldingEntitet(sykepengesoeknad.sykmeldingId) } returns sykmeldingEntitet
            coEvery { agNotifikasjonKlientMock.opprettNySak(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
                UUID.randomUUID().toString()
            coEvery {
                agNotifikasjonKlientMock.opprettNyBeskjed(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns UUID.randomUUID().toString()

            sykepengeSoeknadhandler.oppdaterDialog(sykepengesoeknad)

            coVerify(exactly = 1) { dialogportenClientMock.addTransmission(dialogId, any<TransmissionRequest>()) }
            coVerify(exactly = 1) { agNotifikasjonKlientMock.opprettNySak(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
            coVerify(exactly = 1) {
                agNotifikasjonKlientMock.opprettNyBeskjed(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
            }
        }
    })
