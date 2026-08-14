package no.nav.helsearbeidsgiver.dialogporten.routing

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import java.util.UUID

fun Route.activityRoutes(dialogportenClient: DialogportenClient) {
    get("sett-transmission-lest") {
        println("Motokk HTTP kall sett transmission til lest")
        val dialogId = call.request.queryParameters["dialogId"].toUuidorNull()
        val transmissionId = call.request.queryParameters["transmissionId"].toUuidorNull()

        if (dialogId == null || transmissionId == null) {
            return@get call.respond(HttpStatusCode.BadRequest)
        }

        println("Setter transmission til lest dialog:$dialogId  Transmission:$transmissionId")
        dialogportenClient.markTransmissionOpened(dialogId, transmissionId)
        call.respond(HttpStatusCode.OK)
    }
}

fun String?.toUuidorNull() = this?.let { runCatching { UUID.fromString(it) }.getOrNull() }
