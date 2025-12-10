package no.nav.helsearbeidsgiver.dialogporten

import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.database.InntektsmeldingStatus
import no.nav.helsearbeidsgiver.dialogporten.handlers.ForespoerselHandler
import no.nav.helsearbeidsgiver.dialogporten.handlers.InntektsmeldingHandler
import no.nav.helsearbeidsgiver.dialogporten.handlers.SykepengesoeknadHandler
import no.nav.helsearbeidsgiver.dialogporten.handlers.SykmeldingHandler
import no.nav.helsearbeidsgiver.dialogporten.handlers.UtgaattForespoerselHandler
import no.nav.helsearbeidsgiver.kafka.Inntektsmelding
import no.nav.helsearbeidsgiver.kafka.Inntektsmeldingsforespoersel
import no.nav.helsearbeidsgiver.kafka.Sykepengesoeknad
import no.nav.helsearbeidsgiver.kafka.Sykmelding
import no.nav.helsearbeidsgiver.kafka.UtgaattInntektsmeldingForespoersel
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.util.UUID

class DialogportenService(
    dialogRepository: DialogRepository,
    dialogportenClient: DialogportenClient,
    unleashFeatureToggles: UnleashFeatureToggles,
) {
    private val sykmeldingHandler = SykmeldingHandler(dialogRepository, dialogportenClient, unleashFeatureToggles)
    private val sykepengesoeknadHandler = SykepengesoeknadHandler(dialogRepository, dialogportenClient)
    private val forespoerselHandler = ForespoerselHandler(dialogRepository, dialogportenClient)
    private val inntektsmeldingHandler = InntektsmeldingHandler(dialogRepository, dialogportenClient)
    private val utgaattForespoerselHandler = UtgaattForespoerselHandler(dialogRepository, dialogportenClient)

    fun opprettOgLagreDialog(sykmelding: Sykmelding) {
        sykmeldingHandler.opprettOgLagreDialog(sykmelding)
    }

    fun oppdaterDialogMedSykepengesoeknad(sykepengesoeknad: Sykepengesoeknad) {
        sykepengesoeknadHandler.oppdaterDialog(sykepengesoeknad)
    }

    fun oppdaterDialogMedInntektsmeldingsforespoersel(inntektsmeldingsforespoersel: Inntektsmeldingsforespoersel) {
        oppdaterDialogMedInntektsmeldingsforespoersel(
            forespoerselId = inntektsmeldingsforespoersel.forespoerselId,
            sykmeldingId = inntektsmeldingsforespoersel.sykmeldingId,
        )
    }

    fun oppdaterDialogMedInntektsmeldingsforespoersel(
        forespoerselId: UUID,
        sykmeldingId: UUID,
    ) {
        forespoerselHandler.oppdaterDialog(forespoerselId = forespoerselId, sykmeldingId = sykmeldingId)
    }

    fun oppdaterDialogMedUtgaattForespoersel(utgaattForespoersel: UtgaattInntektsmeldingForespoersel) {
        oppdaterDialogMedUtgaattForespoersel(
            forespoerselId = utgaattForespoersel.forespoerselId,
            sykmeldingId = utgaattForespoersel.sykmeldingId,
        )
    }

    fun oppdaterDialogMedUtgaattForespoersel(
        forespoerselId: UUID,
        sykmeldingId: UUID,
    ) {
        utgaattForespoerselHandler.oppdaterDialog(forespoerselId = forespoerselId, sykmeldingId = sykmeldingId)
    }

    fun oppdaterDialogMedInntektsmelding(
        forespoerselId: UUID,
        sykmeldingId: UUID,
    ) {
        inntektsmeldingHandler.oppdaterDialog(
            Inntektsmelding(
                forespoerselId = forespoerselId,
                sykmeldingId = sykmeldingId,
                // TODO: gjør som andre versjoner og endre til å ikke måtte sende inn dette
                innsendingId = UUID.randomUUID(),
                orgnr = Orgnr("000000000"),
                status = Inntektsmelding.Status.GODKJENT,
                kanal = Inntektsmelding.Kanal.HR_SYSTEM_API,
            ),
        )
    }

    // TODO: DENNE MÅ ENDRES TIL Å IKKE BRUKE INNTEKTSMELDING-KLASSEN
    fun oppdaterDialogMedInntektsmelding(inntektsmelding: Inntektsmelding) {
        inntektsmeldingHandler.oppdaterDialog(inntektsmelding)
    }
}
