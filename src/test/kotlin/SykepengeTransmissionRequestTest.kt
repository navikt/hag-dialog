package no.nav.helsearbeidsgiver.dialogporten

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

class SykepengeTransmissionRequestTest :
    FunSpec({
        test("hentUuidFraFritakKravPdfUrl henter UUID fra kronisk/krav URL") {
            val expected = UUID.fromString("e92343a7-49f7-499a-862c-517ad5477462")
            val url = "https://arbeidsgiver.ekstern.dev.nav.no/dokument/kronisk/krav/e92343a7-49f7-499a-862c-517ad5477462.pdf"

            url.hentUuidFraFritakKravPdfUrl() shouldBe expected
        }
        test("hentUuidFraFritakKravPdfUrl henter UUID fra kronisk-krav URL") {
            val expected = UUID.fromString("e92343a7-49f7-499a-862c-517ad5477462")
            val url = "https://arbeidsgiver.ekstern.dev.nav.no/dokument/kronisk-krav/e92343a7-49f7-499a-862c-517ad5477462.pdf"

            url.hentUuidFraFritakKravPdfUrl() shouldBe expected
        }
        test("hentUuidFraFritakKravPdfUrl henter UUID fra hvilken som helst URL") {
            val expected = UUID.fromString("e92343a7-49f7-499a-862c-517ad5477462")
            val url = "https://example.com/noe/helt/annet/sti?kravId=e92343a7-49f7-499a-862c-517ad5477462"

            url.hentUuidFraFritakKravPdfUrl() shouldBe expected
        }

        test("hentUuidFraFritakKravPdfUrl henter UUID uten .pdf-suffiks") {
            val expected = UUID.fromString("e92343a7-49f7-499a-862c-517ad5477462")
            val url = "prefix-e92343a7-49f7-499a-862c-517ad5477462-suffix"

            url.hentUuidFraFritakKravPdfUrl() shouldBe expected
        }

        test("hentUuidFraFritakKravPdfUrl returnerer null når URL ikke inneholder UUID") {
            val url = "https://arbeidsgiver.ekstern.dev.nav.no/dokument/gravid-soeknad/uten-id.txt"

            url.hentUuidFraFritakKravPdfUrl() shouldBe null
        }
    })
