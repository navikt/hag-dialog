package no.nav.helsearbeidsgiver.helsesjekker

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.naisRoutes(helseSjekkService: HelsesjekkService) {
    route("/health") {
        isAlive()
        isReady(helseSjekkService)
    }
}

private fun Route.isAlive() {
    get("/is-alive") {
        if (ShutDownAppState.shutDownApp) {
            call.respond(HttpStatusCode.InternalServerError, "Not Alive")
        } else {
            call.respond(HttpStatusCode.OK, "Alive")
        }
    }
}

private fun Route.isReady(helseSjekkService: HelsesjekkService) {
    get("/is-ready") {
        if (helseSjekkService.databaseIsAlive() && !ShutDownAppState.shutDownApp) {
            call.respond(HttpStatusCode.OK, "Ready")
        } else {
            call.respond(HttpStatusCode.InternalServerError, "Not Ready")
        }
    }
}
