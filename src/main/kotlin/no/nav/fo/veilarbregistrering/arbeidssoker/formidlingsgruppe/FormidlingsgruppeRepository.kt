package no.nav.fo.veilarbregistrering.arbeidssoker.formidlingsgruppe

import no.nav.fo.veilarbregistrering.arbeidssoker.perioder.Arbeidssokerperioder
import no.nav.fo.veilarbregistrering.bruker.Foedselsnummer

interface FormidlingsgruppeRepository {
    fun lagre(event: FormidlingsgruppeEvent): Long
    fun finnFormidlingsgrupperOgMapTilArbeidssokerperioder(foedselsnummerList: List<Foedselsnummer>): Arbeidssokerperioder
}