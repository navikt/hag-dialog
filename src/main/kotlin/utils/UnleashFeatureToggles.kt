package no.nav.helsearbeidsgiver.utils

import io.getunleash.DefaultUnleash
import io.getunleash.FakeUnleash
import io.getunleash.Unleash
import io.getunleash.util.UnleashConfig
import no.nav.helsearbeidsgiver.Env

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

    fun skalOppretteDialoger(): Boolean =
        defaultUnleash.isEnabled(
            "opprett-dialoger",
            true,
        )

    fun skalOppretteNotifikasjoner(): Boolean =
        defaultUnleash.isEnabled(
            "opprett-notifikasjoner-fager",
            false,
        )
}
