package no.nav.helsearbeidsgiver.utils

import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

// stjålet fra SAS sin spleis kode
// https://github.com/navikt/helse-spleis/blob/main/sykepenger-model/src/main/kotlin/no/nav/helse/UUIDV7.kt

@OptIn(ExperimentalUuidApi::class)
internal fun nyUuidv7(): UUID = Uuid.generateV7().toJavaUuid()

fun UUID.erUuidv7(): Boolean = version() == 7
