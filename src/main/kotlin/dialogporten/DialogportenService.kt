package no.nav.helsearbeidsgiver.dialogporten

import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.dialogporten.handlers.DialogCreator
import no.nav.helsearbeidsgiver.dialogporten.handlers.InntektsmeldingHandler
import no.nav.helsearbeidsgiver.dialogporten.handlers.InntektsmeldingsforespoerselHandler
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
    private val dialogCreator = DialogCreator(dialogRepository, dialogportenClient, unleashFeatureToggles)
    private val sykepengesoeknadHandler = SykepengesoeknadHandler(dialogRepository, dialogportenClient)
    private val inntektsmeldingsforespoerselHandler = InntektsmeldingsforespoerselHandler(dialogRepository, dialogportenClient)
    private val inntektsmeldingHandler = InntektsmeldingHandler(dialogRepository, dialogportenClient)
    private val utgaattForespoerselHandler = UtgaattForespoerselHandler(dialogRepository, dialogportenClient)

    fun opprettOgLagreDialog(sykmelding: Sykmelding) {
        dialogCreator.opprettOgLagreDialog(sykmelding)
    }

    fun oppdaterDialogMedSykepengesoeknad(sykepengesoeknad: Sykepengesoeknad) {
        sykepengesoeknadHandler.oppdaterDialog(sykepengesoeknad)
    }

    fun oppdaterDialogMedInntektsmeldingsforespoersel(inntektsmeldingsforespoersel: Inntektsmeldingsforespoersel) {
        inntektsmeldingsforespoerselHandler.oppdaterDialog(inntektsmeldingsforespoersel)
    }

    fun oppdaterDialogMedInntektsmelding(inntektsmelding: Inntektsmelding) {
        inntektsmeldingHandler.oppdaterDialog(inntektsmelding)
    }

    fun oppdaterDialogMedUtgaattForespoersel(utgaattForespoersel: UtgaattInntektsmeldingForespoersel) {
        utgaattForespoerselHandler.oppdaterDialog(utgaattForespoersel)
    }
}
