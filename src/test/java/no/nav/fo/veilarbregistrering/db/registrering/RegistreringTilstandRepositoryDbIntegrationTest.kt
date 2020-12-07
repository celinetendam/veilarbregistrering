package no.nav.fo.veilarbregistrering.db.registrering

import no.nav.fo.veilarbregistrering.bruker.AktorId
import no.nav.fo.veilarbregistrering.bruker.Bruker
import no.nav.fo.veilarbregistrering.bruker.Foedselsnummer
import no.nav.fo.veilarbregistrering.db.DatabaseConfig
import no.nav.fo.veilarbregistrering.db.MigrationUtils
import no.nav.fo.veilarbregistrering.db.RepositoryConfig
import no.nav.fo.veilarbregistrering.db.TransactionalTest
import no.nav.fo.veilarbregistrering.registrering.bruker.BrukerRegistreringRepository
import no.nav.fo.veilarbregistrering.registrering.bruker.OrdinaerBrukerRegistreringTestdataBuilder
import no.nav.fo.veilarbregistrering.registrering.tilstand.RegistreringTilstand
import no.nav.fo.veilarbregistrering.registrering.tilstand.RegistreringTilstandRepository
import no.nav.fo.veilarbregistrering.registrering.tilstand.RegistreringTilstandTestdataBuilder
import no.nav.fo.veilarbregistrering.registrering.tilstand.Status
import no.nav.fo.veilarbregistrering.registrering.tilstand.Status.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDateTime

@TransactionalTest
@ContextConfiguration(classes = [DatabaseConfig::class, RepositoryConfig::class])
open class RegistreringTilstandRepositoryDbIntegrationTest(

    @Autowired
    private val jdbcTemplate: JdbcTemplate,
    @Autowired
    private val brukerRegistreringRepository: BrukerRegistreringRepository,
    @Autowired
    private val registreringTilstandRepository: RegistreringTilstandRepository) {

    @BeforeEach
    fun setup() {
        MigrationUtils.createTables(jdbcTemplate)
    }

    @Test
    fun `skal kaste DataIntegrityViolationException hvis registreringstilstand lagres uten at registrering er lagret`() {
        val registrering = OrdinaerBrukerRegistreringTestdataBuilder.gyldigBrukerRegistrering()
        assertThrows(DataIntegrityViolationException::class.java) { registreringTilstandRepository.lagre(RegistreringTilstand.medStatus(MOTTATT, registrering.id)) }
    }

    @Test
    fun `skal lagre og hente registreringTilstand`() {
        val registrering = OrdinaerBrukerRegistreringTestdataBuilder.gyldigBrukerRegistrering()
        val lagretRegistrering = brukerRegistreringRepository.lagre(registrering, BRUKER_1)
        val registreringTilstand = RegistreringTilstand.medStatus(MOTTATT, lagretRegistrering.id)
        val id = registreringTilstandRepository.lagre(registreringTilstand)
        assertThat(id).isNotNegative()
        val lagretTilstand = registreringTilstandRepository.hentRegistreringTilstand(id)
        assertThat(lagretTilstand.id).isEqualTo(id)
        assertThat(lagretTilstand.brukerRegistreringId).isEqualTo(lagretRegistrering.id)
        assertThat(lagretTilstand.opprettet).isBetween(LocalDateTime.now().minusSeconds(10), LocalDateTime.now().plusSeconds(10))
        assertThat(lagretTilstand.sistEndret).isNull()
        assertThat(lagretTilstand.status).isEqualTo(MOTTATT)
    }

    @Test
    fun `finnRegistreringTilstandMed skal returnere alle tilstander med angitt status`() {
        val lagretRegistrering1 = brukerRegistreringRepository.lagre(OrdinaerBrukerRegistreringTestdataBuilder.gyldigBrukerRegistrering(), BRUKER_1)
        val tilstand1 = RegistreringTilstandTestdataBuilder.registreringTilstand()
                .brukerRegistreringId(lagretRegistrering1.id)
                .opprettet(LocalDateTime.now().minusMinutes(5))
                .status(MOTTATT)
                .build()

        registreringTilstandRepository.lagre(tilstand1)
        val lagretRegistrering2 = brukerRegistreringRepository.lagre(OrdinaerBrukerRegistreringTestdataBuilder.gyldigBrukerRegistrering(), BRUKER_1)
        val tilstand2 = RegistreringTilstandTestdataBuilder.registreringTilstand()
                .brukerRegistreringId(lagretRegistrering2.id)
                .opprettet(LocalDateTime.now().minusMinutes(5))
                .status(PUBLISERT_KAFKA)
                .build()
        registreringTilstandRepository.lagre(tilstand2)
        val mottatteRegistreringer = registreringTilstandRepository.finnRegistreringTilstanderMed(MOTTATT)
        assertThat(mottatteRegistreringer).hasSize(1)
    }

    @Test
    fun `skal returnere neste registrering klar for publisering`() {
        val nyesteRegistrering = OrdinaerBrukerRegistreringTestdataBuilder.gyldigBrukerRegistrering()
        val eldsteRegistrering = OrdinaerBrukerRegistreringTestdataBuilder.gyldigBrukerRegistrering()
        val lagretNyesteRegistrering = brukerRegistreringRepository.lagre(nyesteRegistrering, BRUKER_1)
        val lagretEldsteRegistrering = brukerRegistreringRepository.lagre(eldsteRegistrering, BRUKER_1)
        val nyesteRegistreringTilstand = RegistreringTilstandTestdataBuilder.registreringTilstand()
                .brukerRegistreringId(lagretNyesteRegistrering.id)
                .opprettet(LocalDateTime.now().minusMinutes(5))
                .status(OVERFORT_ARENA)
                .build()
        registreringTilstandRepository.lagre(nyesteRegistreringTilstand)
        val eldsteRegistreringTilstand = RegistreringTilstandTestdataBuilder.registreringTilstand()
                .brukerRegistreringId(lagretEldsteRegistrering.id)
                .opprettet(LocalDateTime.now().minusMinutes(10))
                .status(OVERFORT_ARENA)
                .build()
        val eldsteRegistreringTilstandId = registreringTilstandRepository.lagre(eldsteRegistreringTilstand)
        val nesteRegistreringKlarForPublisering = registreringTilstandRepository.finnNesteRegistreringTilstandMed(OVERFORT_ARENA)
        assertThat(nesteRegistreringKlarForPublisering.get().id).isEqualTo(eldsteRegistreringTilstandId)
    }

    @Test
    fun `skal returnere empty naar ingen klare for publisering`() {
        val nyesteRegistrering = OrdinaerBrukerRegistreringTestdataBuilder.gyldigBrukerRegistrering()
        val eldsteRegistrering = OrdinaerBrukerRegistreringTestdataBuilder.gyldigBrukerRegistrering()
        val lagretNyesteRegistrering = brukerRegistreringRepository.lagre(nyesteRegistrering, BRUKER_1)
        val lagretEldsteRegistrering = brukerRegistreringRepository.lagre(eldsteRegistrering, BRUKER_1)
        val nyesteRegistreringTilstand = RegistreringTilstandTestdataBuilder.registreringTilstand()
                .brukerRegistreringId(lagretNyesteRegistrering.id)
                .opprettet(LocalDateTime.now().minusMinutes(5))
                .status(PUBLISERT_KAFKA)
                .build()
        registreringTilstandRepository.lagre(nyesteRegistreringTilstand)
        val eldsteRegistreringTilstand = RegistreringTilstandTestdataBuilder.registreringTilstand()
                .brukerRegistreringId(lagretEldsteRegistrering.id)
                .opprettet(LocalDateTime.now().minusMinutes(10))
                .status(PUBLISERT_KAFKA)
                .build()
        registreringTilstandRepository.lagre(eldsteRegistreringTilstand)
        val nesteRegistreringKlarForPublisering = registreringTilstandRepository.finnNesteRegistreringTilstandMed(OVERFORT_ARENA)
        assertThat(nesteRegistreringKlarForPublisering).isEmpty
    }

    @Test
    fun `skal telle antall registreringer med status`() {
        val antallPublisertKafka = 5
        val antallOverfortArena = 3
        lagRegistreringMedTilstand(PUBLISERT_KAFKA, antallPublisertKafka)
        lagRegistreringMedTilstand(OVERFORT_ARENA, antallOverfortArena)
        assertThat(registreringTilstandRepository.hentAntall(PUBLISERT_KAFKA)).isEqualTo(antallPublisertKafka)
        assertThat(registreringTilstandRepository.hentAntall(OVERFORT_ARENA)).isEqualTo(antallOverfortArena)
        assertThat(registreringTilstandRepository.hentAntall(MOTTATT)).isEqualTo(0)
    }

    @Test
    fun `skal kaste exception ved forsoek paa aa lagre tilstand med brukerregistreringid som allerede finnes`() {
        var registrering = OrdinaerBrukerRegistreringTestdataBuilder.gyldigBrukerRegistrering()
        registrering = brukerRegistreringRepository.lagre(registrering, BRUKER_1)
        val registreringTilstandMottatt = RegistreringTilstandTestdataBuilder.registreringTilstand()
                .brukerRegistreringId(registrering.id)
                .opprettet(LocalDateTime.now().minusDays(10))
                .status(MOTTATT)
                .build()
        val registreringTilstandOverfort = RegistreringTilstandTestdataBuilder.registreringTilstand()
                .brukerRegistreringId(registrering.id)
                .opprettet(LocalDateTime.now().minusMinutes(5))
                .status(OVERFORT_ARENA)
                .build()
        registreringTilstandRepository.lagre(registreringTilstandMottatt)
        assertThrows(DuplicateKeyException::class.java) { registreringTilstandRepository.lagre(registreringTilstandOverfort) }
    }

    private fun lagRegistreringMedTilstand(status: Status, antall: Int) {
        for (i in 0 until antall) {
            val registrering = brukerRegistreringRepository.lagre(OrdinaerBrukerRegistreringTestdataBuilder.gyldigBrukerRegistrering(), BRUKER_1)
            val nyesteRegistreringTilstand = RegistreringTilstandTestdataBuilder.registreringTilstand()
                    .brukerRegistreringId(registrering.id)
                    .opprettet(LocalDateTime.now().minusMinutes(5))
                    .status(status)
                    .build()
            registreringTilstandRepository.lagre(nyesteRegistreringTilstand)
        }
    }

    companion object {
        private val FOEDSELSNUMMER = Foedselsnummer.of("12345678911")
        private val AKTOR_ID_11111 = AktorId.of("11111")
        private val BRUKER_1 = Bruker.of(FOEDSELSNUMMER, AKTOR_ID_11111)
    }
}