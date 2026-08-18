package no.nav.helsearbeidsgiver.dialogporten.handlers

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.SakEllerOppgaveDuplikatException
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.Tjeneste
import no.nav.helsearbeidsgiver.arbeidsgivernotifkasjon.graphql.generated.enums.SaksStatus
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.LpsApiExtendedType
import no.nav.helsearbeidsgiver.dialogporten.SykepengesoknadTransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.TransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.createApiAttachment
import no.nav.helsearbeidsgiver.dialogporten.domene.createGuiAttachment
import no.nav.helsearbeidsgiver.kafka.Sykepengesoeknad
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.tilNorskFormat
import java.util.UUID
import kotlin.time.Duration.Companion.days

class SykepengesoeknadHandler(
    private val dialogRepository: DialogRepository,
    private val dialogportenClient: DialogportenClient,
    private val unleashFeatureToggles: UnleashFeatureToggles,
    private val agNotifikasjonKlient: ArbeidsgiverNotifikasjonKlient,
    private val dokumentkoblingRepository: DokumentkoblingRepository,
) {
    private val logger = logger()

    fun oppdaterDialog(sykepengesoeknad: Sykepengesoeknad) {
        val dialog =
            dialogRepository.finnDialogMedSykemeldingId(sykmeldingId = sykepengesoeknad.sykmeldingId)
                ?: run {
                    logger.warn(
                        "Fant ikke dialog for sykmeldingId ${sykepengesoeknad.sykmeldingId}. " +
                            "Klarer derfor ikke oppdatere dialogen med sykepengesøknad ${sykepengesoeknad.soeknadId}.",
                    )
                    return
                }

        val eksisterendeTransmission = dialog.transmissionByDokumentId(sykepengesoeknad.soeknadId)

        if (eksisterendeTransmission != null) {
            logger.info(
                "Transmission for sykepengesøknad ${sykepengesoeknad.soeknadId} " +
                    "finnes allerede i dialog ${dialog.dialogId}, hopper over opprettelse.",
            )
        } else {
            val transmissionId =
                runBlocking {
                    dialogportenClient.removeApiOnly(dialog.dialogId)
                    dialogportenClient.addTransmission(
                        dialogId = dialog.dialogId,
                        transmissionRequest =
                            sykepengesoknadTransmission(
                                soeknadId = sykepengesoeknad.soeknadId,
                            ),
                    )
                }

            dialogRepository.oppdaterDialogMedTransmission(
                sykmeldingId = sykepengesoeknad.sykmeldingId,
                transmissionId = transmissionId,
                dokumentId = sykepengesoeknad.soeknadId,
                dokumentType = LpsApiExtendedType.SYKEPENGESOEKNAD.toString(),
            )

            logger.info(
                "Oppdaterte dialog ${dialog.dialogId} for sykmelding ${sykepengesoeknad.sykmeldingId} " +
                    "med sykepengesøknad ${sykepengesoeknad.soeknadId}. " +
                    "Lagt til transmission $transmissionId.",
            )
        }

        if (unleashFeatureToggles.skalOppretteNotifikasjoner()) {
            val sykmeldingEntitet = dokumentkoblingRepository.hentSykmeldingEntitet(sykepengesoeknad.sykmeldingId)
            if (sykmeldingEntitet == null) {
                logger.warn(
                    "Fant ikke sykmelding ${sykepengesoeknad.sykmeldingId} i databasen. " +
                        "Kan ikke opprette notifikasjoner for sykepengesøknad ${sykepengesoeknad.soeknadId}.",
                )
            } else {
                agNotifikasjonKlient.opprettNotifikasjoner(sykepengesoeknad, sykmeldingEntitet.data)
            }
        }
    }
}

private fun ArbeidsgiverNotifikasjonKlient.opprettNotifikasjoner(
    sykepengesoeknad: Sykepengesoeknad,
    sykmelding: dokumentkobling.Sykmelding,
) {
    val logger = logger()

    val sakTittel =
        "Søknad om sykepenger for ${sykmelding.fulltNavn} (f. ${sykmelding.foedselsdato.tilNorskFormat()})"

    val lenke = "${Env.Nav.arbeidsgiverGuiBaseUrl}/dokument/sykepengesoeknad/${sykepengesoeknad.soeknadId}.pdf"
    val grupperingsid = sykepengesoeknad.soeknadId.toString()

    try {
        val sakId =
            runBlocking {
                this@opprettNotifikasjoner.opprettNySak(
                    virksomhetsnummer = sykepengesoeknad.orgnr.verdi,
                    grupperingsid = grupperingsid,
                    tjeneste = Tjeneste.SOEKNAD,
                    lenke = lenke,
                    tittel = sakTittel,
                    statusTekst = "Mottatt søknad om sykepenger",
                    tilleggsinfo = null,
                    initiellStatus = SaksStatus.MOTTATT,
                    hardDeleteOm = 730.days,
                )
            }
        logger.info("Opprettet notifikasjon-sak $sakId for sykepengesøknad ${sykepengesoeknad.soeknadId}.")
    } catch (e: SakEllerOppgaveDuplikatException) {
        logger.warn("Duplikat sak for sykepengesøknad ${sykepengesoeknad.soeknadId}: ${e.eksisterendeId}")
    } catch (e: Exception) {
        logger.error("Feil ved opprettelse av notifikasjon-sak for sykepengesøknad ${sykepengesoeknad.soeknadId}")
        sikkerLogger().error(
            "Feil ved opprettelse av notifikasjon-sak for sykepengesøknad " +
                "${sykepengesoeknad.soeknadId}",
            e,
        )
        throw e
    }

    try {
        val beskjedId =
            runBlocking {
                this@opprettNotifikasjoner.opprettNyBeskjed(
                    virksomhetsnummer = sykepengesoeknad.orgnr.verdi,
                    eksternId = sykepengesoeknad.soeknadId.toString(),
                    grupperingsid = grupperingsid,
                    tjeneste = Tjeneste.SOEKNAD,
                    lenke = lenke,
                    tekst = "Ny søknad om sykepenger",
                    tidspunkt = null,
                    varslingTittel = "Ny søknad om sykepenger for en av dine ansatte",
                    varslingInnhold =
                        "<p>En ansatt i underenhet med orgnr ${sykepengesoeknad.orgnr.verdi} " +
                            "har sendt inn en søknad om sykepenger.</p>" +
                            "<p>Logg inn på Altinn eller Nav for å se søknaden.</p>" +
                            "<p>Vennlig hilsen Nav.</p>",
                    smsVarslingInnhold =
                        "En ansatt i underenhet med orgnr ${sykepengesoeknad.orgnr.verdi} " +
                            "har sendt inn en søknad om sykepenger. " +
                            "Logg inn på Altinn eller Nav for å se søknaden. Vennlig hilsen Nav.",
                    hardDeleteOm = 730.days,
                )
            }
        logger.info("Opprettet notifikasjon-beskjed $beskjedId for sykepengesøknad ${sykepengesoeknad.soeknadId}.")
    } catch (e: SakEllerOppgaveDuplikatException) {
        logger.warn("Duplikat beskjed for sykepengesøknad ${sykepengesoeknad.soeknadId}: ${e.eksisterendeId}")
    } catch (e: Exception) {
        logger.error("Feil ved opprettelse av notifikasjon-beskjed for sykepengesøknad ${sykepengesoeknad.soeknadId}")
        sikkerLogger().error(
            "Feil ved opprettelse av notifikasjon-beskjed for sykepengesøknad " +
                "${sykepengesoeknad.soeknadId}",
            e,
        )
        throw e
    }
}

fun sykepengesoknadTransmission(
    soeknadId: UUID,
    isSilentUpdate: Boolean = false, // TODO kan fjernes etter engangsjobb patcher transmission
): TransmissionRequest =
    SykepengesoknadTransmissionRequest(
        soeknadId = soeknadId,
        attachments =
            listOf(
                createApiAttachment(
                    "sykepengesoeknad.json",
                    "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/sykepengesoeknad/$soeknadId",
                ),
                createApiAttachment(
                    displayName = "sykepengesoeknad.pdf",
                    url = "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/sykepengesoeknad/$soeknadId/pdf",
                    mediaType = "application/pdf",
                ),
                createGuiAttachment(
                    displayName = "sykepengesoeknad",
                    url = "${Env.Nav.arbeidsgiverGuiBaseUrl}/dokument/sykepengesoeknad/$soeknadId.pdf",
                    mediaType = "application/pdf",
                ),
            ),
        isSilentUpdate = isSilentUpdate,
    )
