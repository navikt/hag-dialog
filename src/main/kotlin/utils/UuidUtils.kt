package no.nav.helsearbeidsgiver.utils

import java.util.UUID

fun String.toUuid(): UUID = UUID.fromString(this)
