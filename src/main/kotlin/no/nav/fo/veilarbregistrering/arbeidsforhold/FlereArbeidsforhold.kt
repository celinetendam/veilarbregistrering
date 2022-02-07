package no.nav.fo.veilarbregistrering.arbeidsforhold

import java.time.LocalDate

data class FlereArbeidsforhold(val flereArbeidsforhold: List<Arbeidsforhold>) {
    /**
     * En bruker som har jobbet sammenhengende i seks av de siste tolv månedene oppfyller betingelsen om arbeidserfaring
     */
    fun harJobbetSammenhengendeSeksAvTolvSisteManeder(dagensDato: LocalDate): Boolean =
        harJobbetSammenhengendeSisteManeder(dagensDato, 6, 12)

    fun harJobbetSammenhengendeSisteManeder(
        dagensDato: LocalDate,
        minAntallMndSammenhengendeJobb: Int,
        antallMnd: Int
    ): Boolean {
        var antallSammenhengendeMandeder = 0
        var mndFraDagensMnd = 0
        var innevaerendeMnd = dagensDato.withDayOfMonth(1)

        while (antallSammenhengendeMandeder < minAntallMndSammenhengendeJobb && mndFraDagensMnd < antallMnd) {
            if (harArbeidsforholdPaaDato(innevaerendeMnd)) {
                antallSammenhengendeMandeder++
            } else {
                antallSammenhengendeMandeder = 0
            }
            innevaerendeMnd = innevaerendeMnd.minusMonths(1)
            mndFraDagensMnd += 1
        }
        return antallSammenhengendeMandeder >= minAntallMndSammenhengendeJobb
    }

    fun sisteUtenDefault(): Arbeidsforhold? {
        return flereArbeidsforhold.minWithOrNull(
            sorterArbeidsforholdEtterTilDato().thenBy(Arbeidsforhold::fom))
    }

    fun siste(): Arbeidsforhold = sisteUtenDefault()
        ?: Arbeidsforhold(null, fom = null, tom = null, navArbeidsforholdId = null)

    fun harArbeidsforholdPaaDato(innevaerendeMnd: LocalDate): Boolean =
        flereArbeidsforhold.any {
            it.erDatoInnenforPeriode(innevaerendeMnd)
        }

    override fun toString(): String = "FlereArbeidsforhold{flereArbeidsforhold=$flereArbeidsforhold}"

    companion object {

        private fun sorterArbeidsforholdEtterTilDato(): Comparator<Arbeidsforhold> {
            return Comparator.comparing(Arbeidsforhold::tom, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed()
        }
    }
}