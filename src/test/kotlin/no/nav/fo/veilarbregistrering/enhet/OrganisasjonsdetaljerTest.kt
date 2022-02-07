package no.nav.fo.veilarbregistrering.enhet

import no.nav.fo.veilarbregistrering.bruker.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OrganisasjonsdetaljerTest {

    @Test
    fun `organisasjonsdetaljer med tomme lister gir ingen kommunenummer`() {
        val organisasjonsdetaljer = Organisasjonsdetaljer()
        assertThat(organisasjonsdetaljer.kommunenummer()).isNull()
    }

    @Test
    fun `organisasjonsdetaljer med null skal handteres som tom liste`() {
        val organisasjonsdetaljer = Organisasjonsdetaljer()
        assertThat(organisasjonsdetaljer.kommunenummer()).isNull()
    }

    @Test
    fun `organisasjonsdetaljer uten åpne adresser gir ingen kommunenummer`() {
        val forretningsadresse = Forretningsadresse(
            Kommune("1234"),
            Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 28))
        )
        val forretningsadresser = listOf(forretningsadresse)
        val postadresse = Postadresse(
            Kommune("1235"),
            Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 28))
        )
        val postadresser = listOf(postadresse)
        val organisasjonsdetaljer = Organisasjonsdetaljer(forretningsadresser, postadresser)
        assertThat(organisasjonsdetaljer.kommunenummer()).isNull()
    }

    @Test
    fun `organisasjonsdetaljer med åpen postadresse skal gi kommunenummer fra postadresse`() {
        val forretningsadresse = Forretningsadresse(
            Kommune("1234"),
            Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 28))
        )
        val forretningsadresser = listOf(forretningsadresse)
        val postadresse = Postadresse(
            Kommune("1235"),
            Periode(LocalDate.of(2020, 1, 1), null)
        )
        val postadresser = listOf(postadresse)
        val organisasjonsdetaljer = Organisasjonsdetaljer(forretningsadresser, postadresser)
        assertThat(organisasjonsdetaljer.kommunenummer()).isEqualTo(Kommune("1235"))
    }

    @Test
    fun `organisasjonsdetaljer med åpen forretningsadresse skal gi kommunenummer fra forretningsadresse`() {
        val forretningsadresse = Forretningsadresse(
            Kommune("1234"),
            Periode(LocalDate.of(2020, 1, 1), null)
        )
        val forretningsadresser = listOf(forretningsadresse)
        val postadresse = Postadresse(
            Kommune("1235"),
            Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 28))
        )
        val postadresser = listOf(postadresse)
        val organisasjonsdetaljer = Organisasjonsdetaljer(forretningsadresser, postadresser)
        assertThat(organisasjonsdetaljer.kommunenummer()).isEqualTo(Kommune("1234"))
    }

    @Test
    fun `organisasjonsdetaljer med apne adresser skal prioritere kommunenummer fra forretningsadresse`() {
        val forretningsadresse = Forretningsadresse(
            Kommune("1234"),
            Periode(LocalDate.of(2020, 1, 1), null)
        )
        val forretningsadresser = listOf(forretningsadresse)
        val postadresse = Postadresse(
            Kommune("1235"),
            Periode(LocalDate.of(2020, 1, 1), null)
        )
        val postadresser = listOf(postadresse)
        val organisasjonsdetaljer = Organisasjonsdetaljer(forretningsadresser, postadresser)
        assertThat(organisasjonsdetaljer.kommunenummer()).isEqualTo(Kommune("1234"))
    }
}
