package no.nav.helsearbeidsgiver.utils

import no.nav.helsearbeidsgiver.kafka.Refusjon
import java.time.LocalDate
import java.time.format.DateTimeFormatter

const val REFUSJON_TITTEL = "Refusjon av sykepenger"

private val norskDatoFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy")

fun LocalDate.tilNorskFormat(): String = format(norskDatoFormat)

fun Refusjon.tittelMedPeriodeFomOgTom(): String =
    REFUSJON_TITTEL + " ${fom.tilNorskFormat()} – ${tom.tilNorskFormat()}" + if (erRevurdering) " (revurdering)" else ""
