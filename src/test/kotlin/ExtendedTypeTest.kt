import io.kotest.core.spec.style.FunSpec
import no.nav.helsearbeidsgiver.dialogporten.FritakAgpType
import no.nav.helsearbeidsgiver.dialogporten.LpsApiExtendedType

class ExtendedTypeTest :
    FunSpec({
        test("ingen duplikat navn mellom LPS API og FritakAGP enums extended type") {
            val navn = LpsApiExtendedType.entries.map { it.name } + FritakAgpType.entries.map { it.name }
            val duplikater =
                navn
                    .groupingBy { it }
                    .eachCount()
                    .filter { it.value > 1 }
                    .keys
            assert(duplikater.isEmpty()) { "Duplikate ExtendedType navn: $duplikater" }
        }
    })
