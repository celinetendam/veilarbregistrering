package no.nav.fo.veilarbregistrering.bruker

import java.time.ZoneId
import no.bekk.bekkopen.person.FodselsnummerCalculator
import java.time.LocalDate
import java.util.*

object FoedselsnummerTestdataBuilder {
    /**
     * Returnerer fødselsnummer til Aremark som er fiktivt
     */
    fun aremark(): Foedselsnummer {
        return Foedselsnummer("10108000398")
    }

    fun getFodselsnummerAsStringOnDateMinusYears(localDate: LocalDate, minusYears: Int): String {
        val date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).minusYears(minusYears.toLong()).toInstant())
        return FodselsnummerCalculator.getFodselsnummerForDate(date).toString()
    }

    fun fodselsnummerOnDateMinusYears(localDate: LocalDate, minusYears: Int): Foedselsnummer {
        val date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).minusYears(minusYears.toLong()).toInstant())
        return Foedselsnummer(FodselsnummerCalculator.getFodselsnummerForDate(date).toString())
    }
}
