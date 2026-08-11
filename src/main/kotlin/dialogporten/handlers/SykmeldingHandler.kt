package no.nav.helsearbeidsgiver.dialogporten.handlers

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.SakEllerOppgaveDuplikatException
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.Tjeneste
import no.nav.helsearbeidsgiver.arbeidsgivernotifkasjon.graphql.generated.enums.SaksStatus
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.SykmeldingTransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.CreateDialogRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.TransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.createApiAttachment
import no.nav.helsearbeidsgiver.dialogporten.domene.createGuiAttachment
import no.nav.helsearbeidsgiver.dialogporten.domene.toTransmission
import no.nav.helsearbeidsgiver.kafka.Sykmelding
import no.nav.helsearbeidsgiver.kafka.getSykmeldingsPerioderString
import no.nav.helsearbeidsgiver.kafka.lagDialogAdditionalInfo
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.tilNorskFormat
import java.util.UUID
import kotlin.time.Duration.Companion.days

class SykmeldingHandler(
    private val dialogRepository: DialogRepository,
    private val dialogportenClient: DialogportenClient,
    private val unleashFeatureToggles: UnleashFeatureToggles,
    private val agNotifikasjonKlient: ArbeidsgiverNotifikasjonKlient,
) {
    private val logger = logger()

    fun opprettOgLagreDialog(sykmelding: Sykmelding) {
        val eksisterendeDialog = dialogRepository.finnDialogMedSykemeldingId(sykmelding.sykmeldingId)

        if (eksisterendeDialog != null) {
            logger.info(
                "Dialog ${eksisterendeDialog.id} finnes allerede for sykmelding ${sykmelding.sykmeldingId}, hopper over opprettelse.",
            )
        } else {
            val dialogId =
                runBlocking {
                    val request =
                        CreateDialogRequest(
                            orgnr = sykmelding.orgnr,
                            externalReference = sykmelding.sykmeldingId.toString(),
                            idempotentKey = sykmelding.sykmeldingId.toString(),
                            title =
                                "Sykepenger for ${sykmelding.fulltNavn} (f. ${sykmelding.foedselsdato.tilNorskFormat()})",
                            summary =
                                sykmelding.sykmeldingsperioder.getSykmeldingsPerioderString(),
                            additionalInfo = lagDialogAdditionalInfo(),
                            transmissions =
                                listOf(
                                    sykmeldingTransmission(sykmelding.sykmeldingId).toTransmission(),
                                ),
                            isApiOnly = false,
                        )

                    dialogportenClient.createDialog(request)
                }
            dialogRepository.lagreDialog(dialogId = dialogId, sykmeldingId = sykmelding.sykmeldingId)
            logger.info("Opprettet dialog $dialogId for sykmelding ${sykmelding.sykmeldingId}.")
        }

        if (unleashFeatureToggles.skalOppretteNotifikasjoner()) {
            opprettNotifikasjoner(sykmelding)
        }
    }

    private fun opprettNotifikasjoner(sykmelding: Sykmelding) {
        val sakTittel = "Sykmelding for ${sykmelding.fulltNavn} (f. ${sykmelding.foedselsdato.tilNorskFormat()})"
        val lenke = "${Env.Nav.arbeidsgiverGuiBaseUrl}/dokument/sykmelding/${sykmelding.sykmeldingId}.pdf"
        val grupperingsid = sykmelding.sykmeldingId.toString()

        try {
            val sakId =
                runBlocking {
                    agNotifikasjonKlient.opprettNySak(
                        virksomhetsnummer = sykmelding.orgnr.verdi,
                        grupperingsid = grupperingsid,
                        tjeneste = Tjeneste.SYKMELDING,
                        lenke = lenke,
                        tittel = sakTittel,
                        statusTekst = "Mottatt sykmelding",
                        tilleggsinfo = sykmelding.sykmeldingsperioder.getSykmeldingsPerioderString(),
                        initiellStatus = SaksStatus.MOTTATT,
                        hardDeleteOm = 730.days,
                    )
                }
            logger.info("Opprettet notifikasjon-sak $sakId for sykmelding ${sykmelding.sykmeldingId}.")
        } catch (e: SakEllerOppgaveDuplikatException) {
            logger.warn("Duplikat sak for sykmelding ${sykmelding.sykmeldingId}: ${e.eksisterendeId}")
        } catch (e: Exception) {
            logger.error("Feil ved opprettelse av notifikasjon-sak for sykmelding ${sykmelding.sykmeldingId}")
            sikkerLogger().error("Feil ved opprettelse av notifikasjon-sak for sykmelding ${sykmelding.sykmeldingId}", e)
            throw e
        }

        try {
            val beskjedId =
                runBlocking {
                    agNotifikasjonKlient.opprettNyBeskjed(
                        virksomhetsnummer = sykmelding.orgnr.verdi,
                        eksternId = sykmelding.sykmeldingId.toString(),
                        grupperingsid = grupperingsid,
                        tjeneste = Tjeneste.SYKMELDING,
                        lenke = lenke,
                        tekst = "Ny sykmelding for en av dine ansatte",
                        tidspunkt = null,
                        varslingTittel = "Ny sykmelding for en av dine ansatte",
                        varslingInnhold =
                            "<p>En ansatt i underenhet ${sykmelding.orgnr.verdi} har sendt inn en ny sykmelding.</p>" +
                                "<p>Logg inn på Altinn eller Nav for å se sykmeldingen.</p>" +
                                "<p>Vennlig hilsen Nav.</p>",
                        smsVarslingInnhold =
                            "En ansatt i underenhet ${sykmelding.orgnr.verdi} har sendt inn en ny sykmelding. " +
                                "Logg inn på Altinn eller Nav for å se sykmeldingen. Vennlig hilsen Nav.",
                        hardDeleteOm = 730.days,
                    )
                }
            logger.info("Opprettet notifikasjon-beskjed $beskjedId for sykmelding ${sykmelding.sykmeldingId}.")
        } catch (e: SakEllerOppgaveDuplikatException) {
            logger.warn("Duplikat beskjed for sykmelding ${sykmelding.sykmeldingId}: ${e.eksisterendeId}")
        } catch (e: Exception) {
            logger.error("Feil ved opprettelse av notifikasjon-beskjed for sykmelding ${sykmelding.sykmeldingId}")
            sikkerLogger().error("Feil ved opprettelse av notifikasjon-beskjed for sykmelding ${sykmelding.sykmeldingId}", e)
            throw e
        }
    }
}

fun sykmeldingTransmission(
    sykmeldingId: UUID,
    isSilentUpdate: Boolean = false, // TODO kan fjernes etter engangsjobb patcher transmission
): TransmissionRequest =
    SykmeldingTransmissionRequest(
        sykmeldingId,
        listOf(
            createApiAttachment(
                displayName = "sykmelding.json",
                url = "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/sykmelding/$sykmeldingId",
            ),
            createApiAttachment(
                displayName = "sykmelding.pdf",
                url = "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/sykmelding/$sykmeldingId/pdf",
                mediaType = "application/pdf",
            ),
            createGuiAttachment(
                displayName = "sykmelding",
                url = "${Env.Nav.arbeidsgiverGuiBaseUrl}/dokument/sykmelding/$sykmeldingId.pdf",
                mediaType = "application/pdf",
            ),
        ),
        isSilentUpdate,
    )
