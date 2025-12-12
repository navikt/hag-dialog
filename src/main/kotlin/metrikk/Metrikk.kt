package no.nav.helsearbeidsgiver.metrikk

import io.micrometer.core.instrument.Gauge
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import java.util.concurrent.atomic.AtomicInteger

val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

val antallMottatteSykmeldinger =
    AtomicInteger(0)
        .also {
            Gauge
                .builder("antall_mottatte_sykmeldinger") { it.get().toDouble() }
                .description("Antall sykmeldinger med status MOTTATT")
                .register(registry)
        }

fun oppdaterMetrikkForAntallMottatteSykmeldinger(nyVerdi: Int) {
    antallMottatteSykmeldinger.set(nyVerdi)
}
