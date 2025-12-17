package no.nav.helsearbeidsgiver.utils

import io.getunleash.DefaultUnleash
import io.getunleash.FakeUnleash
import io.getunleash.Unleash
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

    fun skalOppretteDialoger(): Boolean =
        defaultUnleash.isEnabled(
            "opprett-dialoger",
            false,
        )

    fun skalOppretteDialogKunForApi() =
        defaultUnleash.isEnabled(
            "opprett-dialog-kun-for-api",
            true,
        )
}
