import io.kotest.core.spec.style.FunSpec
import io.ktor.server.routing.routing
import io.ktor.server.testing.TestApplication
import io.mockk.mockk
import no.nav.helsearbeidsgiver.helsesjekker.HelsesjekkService
import no.nav.helsearbeidsgiver.helsesjekker.naisRoutes

abstract class FunSpecWithTestApplication(
    body: FunSpec.(TestApplication, HelsesjekkService) -> Unit,
) : FunSpec({
        val helsesjekkService = mockk<HelsesjekkService>()

        val testApplication =
            TestApplication {
                application {
                    routing {
                        naisRoutes(helsesjekkService)
                    }
                }
            }

        afterSpec {
            testApplication.stop()
        }

        body(testApplication, helsesjekkService)
    })
