package no.nav.fo.veilarbregistrering.db.migrering

import no.nav.fo.veilarbregistrering.db.migrering.TabellNavn.*
import no.nav.fo.veilarbregistrering.log.loggerFor
import no.nav.fo.veilarbregistrering.migrering.MigreringRepository
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.sql.ResultSet

enum class TabellNavn(val idKolonneNavn: String) {
    BRUKER_REGISTRERING("BRUKER_REGISTRERING_ID"),
    BRUKER_PROFILERING("BRUKER_REGISTRERING_ID"),
    BRUKER_REAKTIVERING("BRUKER_REAKTIVERING_ID"),
    SYKMELDT_REGISTRERING("SYKMELDT_REGISTRERING_ID"),
    MANUELL_REGISTRERING("MANUELL_REGISTRERING_ID"),
    REGISTRERING_TILSTAND("ID"),
    OPPGAVE("ID"),
}

class MigreringRepositoryImpl(private val db: NamedParameterJdbcTemplate) : MigreringRepository {

    override fun nesteFraTabell(tabellNavn: TabellNavn, id: Long): List<Map<String, Any>> {
            val sql =
                """
                SELECT *
                FROM ${tabellNavn.name}
                WHERE ${tabellNavn.idKolonneNavn} > :id
                ORDER BY ${tabellNavn.idKolonneNavn}
                FETCH NEXT 1000 ROWS ONLY
                """
            return db.queryForList(sql, mapOf("id" to id))
    }

    override fun hentStatus(): List<Map<String, Any>> {
        val sql =
            """
            select 'registrering_tilstand' as table_name, max(id), count(*) as row_count from registrering_tilstand union 
            select 'bruker_registrering', max(bruker_registrering_id), count(*) as row_count from bruker_registrering union
            select 'bruker_profilering', max(bruker_registrering_id), count(*) as row_count from bruker_profilering union
            select 'bruker_reaktivering', max(bruker_reaktivering_id), count(*) as row_count from bruker_reaktivering union
            select 'sykmeldt_registrering', max(sykmeldt_registrering_id), count(*) as row_count from sykmeldt_registrering union
            select 'manuell_registrering', max(manuell_registrering_id), count(*) as row_count from manuell_registrering union
            select 'oppgave', max(id), count(*) as row_count from oppgave
            """

        return db.queryForList(sql, emptyMap<String, Any>())
    }

    override fun hentSjekksumFor(tabellNavn: TabellNavn): List<Map<String, Any>> {
        val startTime = System.currentTimeMillis()

        val result = when (tabellNavn) {
            BRUKER_PROFILERING -> db.queryForList(profileringSjekkSql, emptyMap<String, Any>())
            BRUKER_REGISTRERING -> db.queryForList(brukerRegistreringSjekkSql, emptyMap<String, Any>())
            SYKMELDT_REGISTRERING -> db.queryForList(sykmeldtRegistreringSjekkSql, emptyMap<String, Any>())
            MANUELL_REGISTRERING -> db.queryForList(manuellRegistreringSjekkSql, emptyMap<String, Any>())
            OPPGAVE -> db.queryForList(oppgaveSjekkSql, emptyMap<String, Any>())
            REGISTRERING_TILSTAND -> db.queryForList(registreringstilstandSjekkSql, emptyMap<String, Any>())
            BRUKER_REAKTIVERING -> db.queryForList(brukerReaktiveringSjekkSql, emptyMap<String, Any>())
        }
        log.info("Sjekksum for {} hentet på {} ms", tabellNavn.name, startTime - System.currentTimeMillis() )
        return result
    }

    override fun hentAntallPotensieltOppdaterte(): Int {
        val sql = "select count(*) as antall from registrering_tilstand " +
                "where status not in ('PUBLISERT_KAFKA', 'OPPRINNELIG_OPPRETTET_UTEN_TILSTAND')"
        return db.queryForObject(
            sql,
            emptyMap<String, Any>()
        ) { rs: ResultSet, _ ->
            rs.getInt("antall")
        }!!
    }

    override fun hentRegistreringTilstander(ider: List<Long>): List<Map<String, Any?>> {
        val sql = "select * from $REGISTRERING_TILSTAND where id in (:idListe)"

        return db.queryForList(sql, mapOf("idListe" to ider))
    }

    companion object {
        private val log = loggerFor<MigreringRepositoryImpl>()

        private const val brukerReaktiveringSjekkSql = """
        select count(*) as antall_rader,
        count(distinct aktor_id) as unike_aktor_id
        from bruker_reaktivering
        """

        private const val registreringstilstandSjekkSql = """
        select count(*) as antall_rader,
        count(distinct bruker_registrering_id) as unike_brukerregistrering_id,
        floor(avg(bruker_registrering_id)) as gjsnitt_bruker_registrering_id 
        from registrering_tilstand
        """
        private const val profileringSjekkSql = """
        select count(*) as antall_rader,
        count(distinct verdi) as unike_verdier, 
        count(distinct profilering_type) as unike_typer 
        from bruker_profilering          
        """

        private const val brukerRegistreringSjekkSql = """
        select count(*) as antall_rader, 
        count(distinct foedselsnummer) as unike_foedselsnummer, 
        count(distinct aktor_id) as unike_aktorer, 
        count(distinct jobbhistorikk) as unike_jobbhistorikk, 
        count(distinct yrkespraksis) as unike_yrkespraksis, 
        floor(avg(konsept_id)) as gjsnitt_konsept_id 
        from bruker_registrering
        """

        private const val sykmeldtRegistreringSjekkSql = """
        select count(*) as antall_rader,
        count(distinct fremtidig_situasjon) as unike_fremtidig_situasjon,
        count(distinct aktor_id) as unike_aktorer,
        count(distinct utdanning_bestatt) as unike_utdanning_bestatt,
        count(distinct andre_utfordringer) as unike_andre_utfordringer,
        round(avg(cast(nus_kode as int)), 2) as gjsnitt_nus from sykmeldt_registrering
        """

        private const val manuellRegistreringSjekkSql = """
        select count(*) as antall_rader,
        count(distinct veileder_ident) as unike_veiledere,
        count(distinct veileder_enhet_id) as unike_enheter,
        count(distinct registrering_id) as unike_registreringer, 
        count(distinct bruker_registrering_type) as unike_reg_typer from manuell_registrering
        """

        private const val oppgaveSjekkSql = """
        select count(*) as antall_rader,
        count(distinct aktor_id) as unike_aktorer,
        count(distinct oppgavetype) as unike_oppgavetyper,
        count(distinct ekstern_oppgave_id) as unike_oppgave_id,
        floor(avg(ekstern_oppgave_id)) as gjsnitt_oppgave_id from oppgave
        """
    }

}

