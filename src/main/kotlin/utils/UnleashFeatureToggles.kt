package no.nav.helsearbeidsgiver.utils

import io.getunleash.DefaultUnleash
import io.getunleash.UnleashContext
import io.getunleash.util.UnleashConfig
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr

class UnleashFeatureToggles {
    private val defaultUnleash: Unleash =
        if (Env.Application.local) {
            FakeUnleash().apply { enableAll() }
        } else {
            DefaultUnleash(
                UnleashConfig
                    .builder()
                    .appName("dialog")
                    .instanceId("dialog")
                    .unleashAPI(Env.Unleash.apiUrl + "/api")
                    .fetchTogglesInterval(5)
                    .apiKey(Env.Unleash.apiKey)
                    .environment(Env.Unleash.apiEnv)
                    .build(),
            )
        }

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
