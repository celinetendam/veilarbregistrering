package no.nav.fo.veilarbregistrering.db.profilering

import no.nav.fo.veilarbregistrering.db.DatabaseConfig
import no.nav.fo.veilarbregistrering.db.RepositoryConfig
import no.nav.fo.veilarbregistrering.profilering.Innsatsgruppe
import no.nav.fo.veilarbregistrering.profilering.Profilering
import no.nav.fo.veilarbregistrering.profilering.ProfileringRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest
import org.springframework.test.context.ContextConfiguration

@JdbcTest
@AutoConfigureTestDatabase(replace= Replace.NONE)
@ContextConfiguration( classes = [ RepositoryConfig::class, DatabaseConfig::class ])
class ProfileringRepositoryDbIntegrationTest(

    @Autowired
    private val profileringRepository: ProfileringRepository) {

    @Test
    fun profilerBruker() {
        val profilering = Profilering(Innsatsgruppe.BEHOV_FOR_ARBEIDSEVNEVURDERING, 39, true)
        profileringRepository.lagreProfilering(9, profilering)

        val profileringFraDb = profileringRepository.hentProfileringForId(9)
        assertThat(profileringFraDb.toString()).isEqualTo(profilering.toString())
    }
}