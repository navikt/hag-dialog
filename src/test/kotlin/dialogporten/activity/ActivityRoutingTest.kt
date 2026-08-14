package no.nav.helsearbeidsgiver.dialogporten.activity

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.TestApplication
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import java.util.UUID

const val LEST_PATH = "/sett-transmission-lest"
val dialogId: UUID = UUID.randomUUID()
val transmissionId: UUID = UUID.randomUUID()

class ActivityRoutingTest :
    FunSpecWithActivityRoutesTestApplication({ testApplication, dialogportenClient ->

        test("skal kalle dialogportenClient.markTransmissionOpened med gitte dialogId og transmissionId") {
            val response =
                testApplication.client.get(
                    "$LEST_PATH?dialogId=$dialogId&transmissionId=$transmissionId",
                )

            response.status shouldBe HttpStatusCode.OK
            coVerify(exactly = 1) { dialogportenClient.markTransmissionOpened(dialogId, transmissionId) }
        }

        test("skal returnere BadRequest når dialogId ikke er en gyldig UUID") {
            val response =
                testApplication.client.get(
                    "$LEST_PATH?dialogId=ikke-en-uuid&transmissionId=$transmissionId",
                )

            response.status shouldBe HttpStatusCode.BadRequest
            coVerify(exactly = 0) { dialogportenClient.markTransmissionOpened(any(), any()) }
        }

        test("skal returnere BadRequest når transmissionId ikke er en gyldig UUID") {
            val response =
                testApplication.client.get(
                    "$LEST_PATH?dialogId=$dialogId&transmissionId=ikke-en-uuid",
                )

            response.status shouldBe HttpStatusCode.BadRequest
            coVerify(exactly = 0) { dialogportenClient.markTransmissionOpened(any(), any()) }
        }
    })

abstract class FunSpecWithActivityRoutesTestApplication(
    body: FunSpec.(TestApplication, DialogportenClient) -> Unit,
) : FunSpec({
        val dialogportenClient = mockk<DialogportenClient>(relaxed = true)

        beforeTest {
            clearMocks(dialogportenClient)
        }

        val testApplication =
            TestApplication {
                application {
                    routing {
                        activityRoutes(dialogportenClient)
                    }
                }
            }

        afterSpec {
            testApplication.stop()
        }

        body(testApplication, dialogportenClient)
    })
