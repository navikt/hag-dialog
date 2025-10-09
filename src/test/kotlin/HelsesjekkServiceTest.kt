import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.helsesjekker.HelsesjekkService
import org.jetbrains.exposed.sql.transactions.TransactionManager

class HelsesjekkServiceTest :
    FunSpecWithDb(emptyList(), { db ->
        val helsesjekkService = HelsesjekkService(db)

        test("databaseIsAlive skal returnere true når database er tilgjengelig") {
            helsesjekkService.databaseIsAlive() shouldBe true
        }

        test("databaseIsAlive skal returnere false når database ikke er tilgjengelig") {
            // Lukk databaseconnection for å simulere at databasen er nede
            TransactionManager.closeAndUnregister(db)

            helsesjekkService.databaseIsAlive() shouldBe false
        }
    })
