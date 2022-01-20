package no.nav.fo.veilarbregistrering.db

import io.mockk.every
import io.mockk.mockk
import no.nav.fo.veilarbregistrering.besvarelse.StillingTestdataBuilder.gyldigStilling
import no.nav.fo.veilarbregistrering.bruker.AktorId
import no.nav.fo.veilarbregistrering.bruker.Bruker
import no.nav.fo.veilarbregistrering.bruker.Foedselsnummer
import no.nav.fo.veilarbregistrering.enhet.Kommune
import no.nav.fo.veilarbregistrering.enhet.Kommune.KommuneMedBydel.STAVANGER
import no.nav.fo.veilarbregistrering.oppfolging.OppfolgingGateway
import no.nav.fo.veilarbregistrering.oppfolging.Oppfolgingsstatus
import no.nav.fo.veilarbregistrering.orgenhet.Enhetnr
import no.nav.fo.veilarbregistrering.orgenhet.NavEnhet
import no.nav.fo.veilarbregistrering.orgenhet.Norg2Gateway
import no.nav.fo.veilarbregistrering.profilering.ProfileringRepository
import no.nav.fo.veilarbregistrering.profilering.ProfileringService
import no.nav.fo.veilarbregistrering.profilering.ProfileringTestdataBuilder.lagProfilering
import no.nav.fo.veilarbregistrering.registrering.bruker.*
import no.nav.fo.veilarbregistrering.registrering.formidling.RegistreringTilstand
import no.nav.fo.veilarbregistrering.registrering.formidling.RegistreringTilstandRepository
import no.nav.fo.veilarbregistrering.registrering.formidling.Status
import no.nav.fo.veilarbregistrering.registrering.manuell.ManuellRegistreringRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.util.*

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = [RepositoryConfig::class, DatabaseConfig::class, HentBrukerRegistreringServiceIntegrationTest.Companion.TestContext::class])
class HentBrukerRegistreringServiceIntegrationTest(
    @Autowired val brukerRegistreringRepository: BrukerRegistreringRepository,
    @Autowired val registreringTilstandRepository: RegistreringTilstandRepository,
    @Autowired val hentRegistreringService: HentRegistreringService,
    @Autowired var oppfolgingGateway: OppfolgingGateway,
    @Autowired var profileringService: ProfileringService
) {

    @BeforeEach
    fun setupEach() {
        every { oppfolgingGateway.hentOppfolgingsstatus(any()) } returns Oppfolgingsstatus(
            false,
            false,
            null,
            null,
            null,
            null
        )
        every { profileringService.profilerBruker(any(), any(), any()) } returns lagProfilering()
    }

    @Test
    fun `henter opp siste brukerregistrering med filtre på tilstand`() {
        brukerRegistreringRepository.lagre(SELVGAENDE_BRUKER, BRUKER).id.let { id ->
            registreringTilstandRepository.lagre(RegistreringTilstand.medStatus(Status.OVERFORT_ARENA, id))
        }
        brukerRegistreringRepository.lagre(BRUKER_UTEN_JOBB, BRUKER).id.let { id ->
            registreringTilstandRepository.lagre(RegistreringTilstand.medStatus(Status.OVERFORT_ARENA, id))
        }
        assertEquals(gyldigStilling(), hentRegistreringService.hentOrdinaerBrukerRegistrering(BRUKER).sisteStilling)
    }

    companion object {
        private val ident = Foedselsnummer.of("10108000398") //Aremark fiktivt fnr.";
        private val BRUKER = Bruker(ident, AktorId("AKTØRID"))
        private val BRUKER_UTEN_JOBB = OrdinaerBrukerRegistreringTestdataBuilder.gyldigBrukerRegistreringUtenJobb(opprettetDato =
            LocalDate.of(2014, 12, 8).atStartOfDay())
        private val SELVGAENDE_BRUKER = OrdinaerBrukerRegistreringTestdataBuilder.gyldigBrukerRegistrering(
            opprettetDato = LocalDate.of(2018, 12, 8).atStartOfDay()
        )

        @Configuration
        class TestContext {
            @Bean
            fun hentRegistreringService(
                    db: JdbcTemplate,
                    brukerRegistreringRepository: BrukerRegistreringRepository,
                    sykmeldtRegistreringRepository: SykmeldtRegistreringRepository,
                    profileringRepository: ProfileringRepository,
                    manuellRegistreringRepository: ManuellRegistreringRepository
            ) = HentRegistreringService(
                    brukerRegistreringRepository,
                    sykmeldtRegistreringRepository,
                    profileringRepository,
                    manuellRegistreringRepository,
                    norg2Gateway()
            )

            @Bean
            fun hentBrukerTilstandService(
                    oppfolgingGateway: OppfolgingGateway,
                    brukerRegistreringRepository: BrukerRegistreringRepository,
            ): BrukerTilstandService {
                return BrukerTilstandService(
                    oppfolgingGateway,
                    brukerRegistreringRepository
                )
            }

            @Bean
            fun oppfolgingGateway(): OppfolgingGateway = mockk()

            @Bean
            fun profileringService(): ProfileringService = mockk()

            @Bean
            fun norg2Gateway() = object : Norg2Gateway {
                override fun hentEnhetFor(kommune: Kommune): Optional<Enhetnr> {
                    if (Kommune("1241") == kommune) {
                        return Optional.of(Enhetnr("232"))
                    }
                    return if (Kommune.medBydel(STAVANGER) == kommune) {
                        Optional.of(Enhetnr("1103"))
                    } else Optional.empty()
                }

                override fun hentAlleEnheter(): Map<Enhetnr, NavEnhet> = emptyMap()
            }
        }
    }
}
