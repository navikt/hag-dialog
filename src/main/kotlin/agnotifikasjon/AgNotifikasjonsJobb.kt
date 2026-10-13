package no.nav.helsearbeidsgiver.notifikasjon

import kotlinx.coroutines.runBlocking
import no.nav.hag.utils.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbStatus
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.Tjeneste
import no.nav.helsearbeidsgiver.utils.json.fromJson
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

class AgNotifikasjonsJobb(
    private val agNotifikasjonService: AgNotifikasjonService,
) : BakgrunnsjobbProsesserer {
    companion object {
        const val JOB_TYPE = "AgNotifikasjonsJobb"
    }

    private val logger = logger()
    private val sikkerLogger = sikkerLogger()

    override val type: String get() = JOB_TYPE

    override fun prosesser(jobb: Bakgrunnsjobb) {
        runBlocking {
            behandleNotifikasjoner(jobb)
        }
    }

    private suspend fun behandleNotifikasjoner(jobb: Bakgrunnsjobb) {
        val data =
            jobb.dataJson ?: run {
                logger.error("Bakgrunnsjobb ${jobb.uuid} mangler data.")
                jobb.status = BakgrunnsjobbStatus.AVBRUTT
                return
            }
        val agNotifikasjon =
            try {
                data.fromJson(AgNotifikasjon.serializer())
            } catch (e: Exception) {
                logger.error("Bakgrunnsjobb ${jobb.uuid} har ugyldig data.", e)
                jobb.status = BakgrunnsjobbStatus.AVBRUTT
                return
            }

        logger.info(
            "Behandler notifikasjon med dokumentId ${agNotifikasjon.dokumentId} og tjeneste ${agNotifikasjon.tjeneste}.",
        )
        try {
            when (agNotifikasjon.tjeneste) {
                Tjeneste.SYKMELDING -> {
                    agNotifikasjonService.opprettNotifikasjonerForSykmelding(agNotifikasjon.dokumentId)
                }

                Tjeneste.SOEKNAD -> {
                    agNotifikasjonService.opprettNotifikasjonerForSoeknad(agNotifikasjon.dokumentId)
                }

                else -> {
                    logger.error(
                        "Bakgrunnsjobb ${jobb.uuid} av type ${jobb.type} har ukjent tjeneste ${agNotifikasjon.tjeneste}.",
                    )
                    jobb.status = BakgrunnsjobbStatus.AVBRUTT
                    return
                }
            }
        } catch (e: DokumentIkkeFunnetException) {
            logger.warn("${e.message} Prøver igjen senere.")
            throw e
        } catch (e: Exception) {
            "Feil ved behandling av notifikasjon for tjeneste ${agNotifikasjon.tjeneste} og dokument ${agNotifikasjon.dokumentId}".also {
                logger.error(it)
                sikkerLogger.error(it, e)
            }
            throw e
        }
    }
}
