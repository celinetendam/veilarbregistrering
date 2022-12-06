package no.nav.fo.veilarbregistrering.arbeidssoker.formidlingsgruppe.adapter

import java.time.LocalDate

data class FormidlingshistorikkDto(
    val formidlingsgruppeKode: String,
    val modDato: String,
    val fraDato: LocalDate,
    val tilDato: LocalDate?
)