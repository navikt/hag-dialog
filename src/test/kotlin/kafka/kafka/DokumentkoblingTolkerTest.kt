package kafka.kafka

import dokumentkobling.Dokumentkobling
import dokumentkobling.DokumentkoblingService
import io.getunleash.FakeUnleash
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository
import no.nav.helsearbeidsgiver.database.SykmeldingEntity
import no.nav.helsearbeidsgiver.kafka.kafka.DokumentkoblingTolker
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.json.toJson

class DokumentkoblingTolkerTest :
    FunSpec({

        test("les melding skal opprette sykmelding i databasen hvis den ikke finnes, og ikke opprette hvis den allerede finnes") {
            val dokumentkoblingRepository = mockk<DokumentkoblingRepository>(relaxed = true)
            val dokumentkoblingService = DokumentkoblingService(dokumentkoblingRepository)

            val dokumentkoblingTolker =
                DokumentkoblingTolker(
                    unleashFeatureToggles = UnleashFeatureToggles(),
                    dokumentkoblingService = dokumentkoblingService,
                )

            every { dokumentkoblingRepository.hentSykmeldingEntitet(any()) } returns null andThen mockk<SykmeldingEntity>()

            dokumentkoblingTolker.lesMelding(
                DokumentKoblingMockUtils.sykmelding.toJson(Dokumentkobling.serializer()).toString(),
            )

            verify(exactly = 1) { dokumentkoblingRepository.opprettSykmelding(any()) }
            dokumentkoblingTolker.lesMelding(
                DokumentKoblingMockUtils.sykmelding.toJson(Dokumentkobling.serializer()).toString(),
            )
            verify(exactly = 1) { dokumentkoblingRepository.opprettSykmelding(any()) }
        }
    })
