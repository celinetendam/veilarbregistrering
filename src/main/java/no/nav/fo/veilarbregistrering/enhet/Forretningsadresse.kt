package no.nav.fo.veilarbregistrering.enhet

import no.nav.fo.veilarbregistrering.bruker.Periode

class Forretningsadresse(override val kommunenummer: Kommune, private val periode: Periode) : Adresse {
    override fun erGyldig(): Boolean = periode.erApen()
}