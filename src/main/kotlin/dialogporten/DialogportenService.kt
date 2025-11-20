package no.nav.helsearbeidsgiver.dialogporten

import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.dialogporten.handlers.SykmeldingHandler
import no.nav.helsearbeidsgiver.dialogporten.handlers.InntektsmeldingHandler
import no.nav.helsearbeidsgiver.dialogporten.handlers.ForespoerselHandler
import no.nav.helsearbeidsgiver.dialogporten.handlers.SykepengesoeknadHandler
import no.nav.helsearbeidsgiver.dialogporten.handlers.UtgaattForespoerselHandler
import no.nav.helsearbeidsgiver.kafka.Inntektsmelding
import no.nav.helsearbeidsgiver.kafka.Inntektsmeldingsforespoersel
import no.nav.helsearbeidsgiver.kafka.Sykepengesoeknad
import no.nav.helsearbeidsgiver.kafka.Sykmelding
import no.nav.helsearbeidsgiver.kafka.UtgaattInntektsmeldingForespoersel
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles

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
        forespoerselHandler.oppdaterDialog(inntektsmeldingsforespoersel)
    }

    fun oppdaterDialogMedInntektsmelding(inntektsmelding: Inntektsmelding) {
        inntektsmeldingHandler.oppdaterDialog(inntektsmelding)
    }

    fun oppdaterDialogMedUtgaattForespoersel(utgaattForespoersel: UtgaattInntektsmeldingForespoersel) {
        utgaattForespoerselHandler.oppdaterDialog(utgaattForespoersel)
    }
}
