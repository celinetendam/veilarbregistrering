package no.nav.fo.veilarbregistrering.bruker.pdl.endepunkt

import no.nav.fo.veilarbregistrering.bruker.pdl.PdlError
import java.time.LocalDate
import java.util.*

data class PdlHentPerson(val hentPerson: PdlPerson)


data class PdlPerson(
    val telefonnummer: List<PdlTelefonnummer>,
    val foedsel: List<PdlFoedsel> = emptyList(),
    val adressebeskyttelse: List<PdlAdressebeskyttelse> = emptyList(),
    val navn: List<PdlNavn> = emptyList()) {

    fun hoyestPrioriterteTelefonnummer(): PdlTelefonnummer? =
        telefonnummer.minOrNull()

    fun getSistePdlFoedsel() =
        if (foedsel.isEmpty()) Optional.empty()
        else Optional.of(foedsel[foedsel.size - 1])

    fun strengesteAdressebeskyttelse() =
        (if (adressebeskyttelse.isEmpty()) Optional.empty()
        else adressebeskyttelse.stream()
            .sorted()
            .findFirst())!!

    fun getNavn() = navn.first()
}
data class PdlNavn(val fornavn: String, val mellomnavn:String?, val etternavn:String)

data class PdlTelefonnummer(val nummer: String,
                            val landskode: String? = null,
                            val prioritet: Int = 0) : Comparable<PdlTelefonnummer> {

    override operator fun compareTo(other: PdlTelefonnummer): Int {
        if (prioritet > other.prioritet) {
            return 1
        }
        return if (prioritet < other.prioritet) {
            -1
        } else 0
    }
}
data class PdlFoedsel(val foedselsdato: LocalDate)

enum class PdlGradering(internal val niva: Int) {
    STRENGT_FORTROLIG_UTLAND(3),  // Tilsvarer paragraf 19 i Bisys (henvisning til Forvaltningslovens §19)
    STRENGT_FORTROLIG(2),  // Tidligere spesregkode kode 6 fra TPS
    FORTROLIG(1),  // Tidligere spesregkode kode 7 fra TPS
    UGRADERT(0);
}

data class PdlAdressebeskyttelse(val gradering: PdlGradering) : Comparable<PdlAdressebeskyttelse> {
    override operator fun compareTo(other: PdlAdressebeskyttelse) = other.gradering.niva - gradering.niva
}


data class PdlHentPersonRequest(val query: String, val variables: HentPersonVariables)

data class PdlHentPersonResponse(val data: PdlHentPerson)

data class HentPersonVariables(val ident: String, val oppholdHistorikk: Boolean)

data class PdlErrorResponse(val errors: List<PdlError> = emptyList())


