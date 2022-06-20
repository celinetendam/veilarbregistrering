package no.nav.fo.veilarbregistrering.db.arbeidssoker

import no.nav.fo.veilarbregistrering.arbeidssoker.FormidlingsgruppeRepository
import no.nav.fo.veilarbregistrering.arbeidssoker.Arbeidssokerperioder
import no.nav.fo.veilarbregistrering.arbeidssoker.EndretFormidlingsgruppeCommand
import no.nav.fo.veilarbregistrering.arbeidssoker.Formidlingsgruppe
import no.nav.fo.veilarbregistrering.bruker.Foedselsnummer
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.sql.Timestamp
import java.time.LocalDateTime

class FormidlingsgruppeRepositoryImpl(private val db: NamedParameterJdbcTemplate) : FormidlingsgruppeRepository {

    override fun lagre(command: EndretFormidlingsgruppeCommand): Long {
        val personId = command.personId
        val fnr = command.foedselsnummer?.stringValue()
            ?: throw IllegalStateException("Foedselsnummer var ikke satt. Skulle vært filtrert bort i forkant!")

        val formidlingsgruppe = command.formidlingsgruppe.kode
        val formidlingsgruppeEndret = Timestamp.valueOf(command.formidlingsgruppeEndret)

        if (erAlleredePersistentLagret(personId, formidlingsgruppe, formidlingsgruppeEndret)) {
            LOG.info("Endringen er allerede lagret, denne forkastes. PersonID:" +
                    " $personId, Formidlingsgruppe: $formidlingsgruppe, Endret: $formidlingsgruppeEndret"
                )
            return -1
        }

        val id = nesteFraSekvens()
        val params = mapOf(
            "id" to id,
            "fnr" to fnr,
            "person_id" to personId,
            "person_id_status" to command.personIdStatus,
            "operasjon" to command.operation.name,
            "formidlingsgruppe" to formidlingsgruppe,
            "formidlingsgruppe_endret" to formidlingsgruppeEndret,
            "forrige_formidlingsgruppe" to command.forrigeFormidlingsgruppe?.let(Formidlingsgruppe::kode),
            "forrige_formidlingsgruppe_endret" to command.forrigeFormidlingsgruppeEndret?.let(Timestamp::valueOf),
            "formidlingsgruppe_lest" to Timestamp.valueOf(LocalDateTime.now())
        )
        val sql = "INSERT INTO $FORMIDLINGSGRUPPE ($ID, $FOEDSELSNUMMER, $PERSON_ID, $PERSON_ID_STATUS, $OPERASJON," +
                " $FORMIDLINGSGRUPPE, $FORMIDLINGSGRUPPE_ENDRET, $FORR_FORMIDLINGSGRUPPE," +
                " $FORR_FORMIDLINGSGRUPPE_ENDRET, $FORMIDLINGSGRUPPE_LEST)" +
                " VALUES (:id, :fnr, :person_id, :person_id_status, :operasjon, :formidlingsgruppe, " +
                " :formidlingsgruppe_endret, :forrige_formidlingsgruppe, :forrige_formidlingsgruppe_endret," +
                " :formidlingsgruppe_lest)"
        try {
            db.update(sql, params)
        } catch (e: DataIntegrityViolationException) {
            throw DataIntegrityViolationException("Lagring av følgende formidlingsgruppeendring feilet: $command", e)
        }
        return id
    }

    private fun erAlleredePersistentLagret(personID: String, formidlingsgruppe: String, endret: Timestamp): Boolean {
        val params = mapOf("person_id" to personID, "formidlingsgruppe" to formidlingsgruppe, "endret" to endret)

        val sql = "SELECT * FROM $FORMIDLINGSGRUPPE " +
                " WHERE $PERSON_ID = :person_id " +
                " AND $FORMIDLINGSGRUPPE = :formidlingsgruppe " +
                " AND $FORMIDLINGSGRUPPE_ENDRET = :endret"

        val formidlingsgruppeendringer = db.query(sql, params, fgruppeMapper)
        return formidlingsgruppeendringer.isNotEmpty()
    }

    private fun nesteFraSekvens(): Long {
        return db.queryForObject("SELECT $FORMIDLINGSGRUPPE_SEQ.nextval FROM DUAL", emptyMap<String, Any>(), Long::class.java)!!
    }

    override fun finnFormidlingsgrupper(foedselsnummerList: List<Foedselsnummer>): Arbeidssokerperioder {
        val sql = "SELECT * FROM $FORMIDLINGSGRUPPE WHERE $FOEDSELSNUMMER IN (:foedselsnummer)"
        val parameters = mapOf("foedselsnummer" to foedselsnummerList.map(Foedselsnummer::stringValue))

        val formidlingsgruppeendringer =
            db.query(sql, parameters, fgruppeMapper)
        LOG.info(
            String.format(
                "Fant følgende rådata med formidlingsgruppeendringer: %s",
                formidlingsgruppeendringer.toString()
            )
        )
        return ArbeidssokerperioderMapper.map(formidlingsgruppeendringer)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(FormidlingsgruppeRepositoryImpl::class.java)
        const val FORMIDLINGSGRUPPE_SEQ = "FORMIDLINGSGRUPPE_SEQ"
        const val FORMIDLINGSGRUPPE = "FORMIDLINGSGRUPPE"
        const val ID = "ID"
        const val FOEDSELSNUMMER = "FOEDSELSNUMMER"
        const val PERSON_ID = "PERSON_ID"
        const val PERSON_ID_STATUS = "PERSON_ID_STATUS"
        const val OPERASJON = "OPERASJON"
        private const val FORMIDLINGSGRUPPE_ENDRET = "FORMIDLINGSGRUPPE_ENDRET"
        private const val FORR_FORMIDLINGSGRUPPE = "FORR_FORMIDLINGSGRUPPE"
        private const val FORR_FORMIDLINGSGRUPPE_ENDRET = "FORR_FORMIDLINGSGRUPPE_ENDRET"
        private const val FORMIDLINGSGRUPPE_LEST = "FORMIDLINGSGRUPPE_LEST"

        private val fgruppeMapper = RowMapper { rs, _ ->
            Formidlingsgruppeendring(
                rs.getString(FORMIDLINGSGRUPPE),
                rs.getInt(PERSON_ID),
                rs.getString(PERSON_ID_STATUS),
                rs.getTimestamp(FORMIDLINGSGRUPPE_ENDRET)
            )
        }
    }
}