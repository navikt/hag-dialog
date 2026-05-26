package no.nav.helsearbeidsgiver.utils

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@OptIn(ExperimentalUuidApi::class)
internal fun nyUuidv7() = Uuid.generateV7().toJavaUuid()
