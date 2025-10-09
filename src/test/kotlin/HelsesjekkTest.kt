import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.mockk.every
import no.nav.helsearbeidsgiver.helsesjekker.ShutDownAppState

class HelsesjekkTest :
    FunSpecWithTestApplication({ testApplication, helsesjekkService ->
        test("skal returnere 200 OK for /health/is-alive") {
            val response = testApplication.client.get("/health/is-alive")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldBe "Alive"
        }

        test("skal returnere 200 OK for /health/is-ready når databasen er oppe") {
            every { helsesjekkService.databaseIsAlive() } returns true
            val response = testApplication.client.get("/health/is-ready")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldBe "Ready"
        }

        test("skal returnere 500 InternalServerError for /health/is-ready når databasen er nede") {
            every { helsesjekkService.databaseIsAlive() } returns false
            val response = testApplication.client.get("/health/is-ready")
            response.status shouldBe HttpStatusCode.InternalServerError
            response.bodyAsText() shouldBe "Not Ready"
        }

        test("skal returnere 500 InternalServerError for /health/is-alive når shutDownApp er true") {
            ShutDownAppState.shutDownApp = true
            val response = testApplication.client.get("/health/is-alive")
            response.status shouldBe HttpStatusCode.InternalServerError
            response.bodyAsText() shouldBe "Not Alive"
        }

        test("skal returnere 500 InternalServerError for /health/is-ready når shutDownApp er true") {
            ShutDownAppState.shutDownApp = true
            val response = testApplication.client.get("/health/is-ready")
            response.status shouldBe HttpStatusCode.InternalServerError
            response.bodyAsText() shouldBe "Not Ready"
        }
    })
