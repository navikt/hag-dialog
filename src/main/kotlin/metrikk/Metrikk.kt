package no.nav.helsearbeidsgiver.metrikk

import io.micrometer.core.instrument.Gauge
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import java.util.concurrent.atomic.AtomicInteger

val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

val antallSykmeldingerMedStatusMottatt =
    AtomicInteger(0)
        .also {
            Gauge
                .builder("hag_dialog_antall_sykmeldinger_med_status_mottatt") { it.get().toDouble() }
                .description("Antall sykmeldinger med status MOTTATT")
                .register(registry)
        }

val antallSykepengesoeknaderMedStatusMottatt =
    AtomicInteger(0)
        .also {
            Gauge
                .builder("hag_dialog_antall_sykepengesoeknader_med_status_mottatt") { it.get().toDouble() }
                .description("Antall sykepengesøknader med status MOTTATT")
                .register(registry)
        }

val antallForespoerslerMedStatusMottatt =
    AtomicInteger(0)
        .also {
            Gauge
                .builder("hag_dialog_antall_forespoersler_med_status_mottatt") { it.get().toDouble() }
                .description("Antall forespørsler med status MOTTATT")
                .register(registry)
        }

val antallInntektsmeldingerMedStatusMottatt =
    AtomicInteger(0)
        .also {
            Gauge
                .builder("hag_dialog_antall_inntektsmeldinger_med_status_mottatt") { it.get().toDouble() }
                .description("Antall inntektsmeldinger med status MOTTATT")
                .register(registry)
        }

fun oppdaterMetrikkForAntallSykmeldingerMedStatusMottatt(nyVerdi: Int) {
    antallSykmeldingerMedStatusMottatt.set(nyVerdi)
}

fun oppdaterMetrikkForAntallSykepengesoeknaderMedStatusMottatt(nyVerdi: Int) {
    antallSykepengesoeknaderMedStatusMottatt.set(nyVerdi)
}

fun oppdaterMetrikkForAntallForespoerslerMedStatusMottatt(nyVerdi: Int) {
    antallForespoerslerMedStatusMottatt.set(nyVerdi)
}

fun oppdaterMetrikkForAntallInntektsmeldingerMedStatusMottatt(nyVerdi: Int) {
    antallInntektsmeldingerMedStatusMottatt.set(nyVerdi)
}
