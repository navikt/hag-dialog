package no.nav.helsearbeidsgiver.utils

import io.getunleash.DefaultUnleash
import io.getunleash.UnleashContext
import io.getunleash.util.UnleashConfig
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr

class UnleashFeatureToggles {
    private val apiKey = Env.Unleash.apiKey
    private val apiUrl = Env.Unleash.apiUrl + "/api"
    private val apiEnv = Env.Unleash.apiEnv

    private val defaultUnleash: DefaultUnleash =
        DefaultUnleash(
            UnleashConfig
                .builder()
                .appName("dialog")
                .instanceId("dialog")
                .unleashAPI(apiUrl)
                .fetchTogglesInterval(5)
                .apiKey(apiKey)
                .environment(apiEnv)
                .build(),
        )

    fun skalOppretteDialogVedMottattSykmelding(orgnr: Orgnr): Boolean =
        defaultUnleash.isEnabled(
            "opprett-dialog-ved-mottatt-sykmelding",
            UnleashContext.builder().addProperty("orgnr", orgnr.toString()).build(),
            false,
        )

    fun skalOppdatereDialogVedMottattSoeknad(orgnr: Orgnr): Boolean =
        defaultUnleash.isEnabled(
            "oppdater-dialog-ved-mottatt-soknad",
            UnleashContext.builder().addProperty("orgnr", orgnr.toString()).build(),
            false,
        )

    fun skalOppdatereDialogVedMottattInntektsmeldingsforespoersel(orgnr: Orgnr): Boolean =
        defaultUnleash.isEnabled(
            "forespor-inntektsmelding-via-dialogporten",
            UnleashContext.builder().addProperty("orgnr", orgnr.toString()).build(),
            false,
        )

    fun skalOppdatereDialogVedMottattInntektsmelding(orgnr: Orgnr): Boolean =
        defaultUnleash.isEnabled(
            "oppdater-dialog-ved-mottatt-inntektsmelding",
            UnleashContext.builder().addProperty("orgnr", orgnr.toString()).build(),
            false,
        )

    fun skalOppretteDialogKunForApi() =
        defaultUnleash.isEnabled(
            "opprett-dialog-kun-for-api",
            true,
        )
}
