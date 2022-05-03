package no.nav.fo.veilarbregistrering.arbeidsforhold.adapter

import no.nav.fo.veilarbregistrering.arbeidsforhold.Arbeidsforhold
import java.time.LocalDate

internal object ArbeidsforholdMapperV2 {
    fun map(arbeidsforholdDto: ArbeidsforholdDto): Arbeidsforhold = Arbeidsforhold(
            arbeidsgiverOrgnummer = arbeidsforholdDto.arbeidsgiver?.let(::orgnummerHvisOrganisasjon),
            styrk = arbeidsforholdDto.arbeidsavtaler.sisteArbeidsavtale()
                    ?.let(ArbeidsavtaleDto::yrke)
                    ?: "utenstyrkkode",
            fom = getFom(arbeidsforholdDto.ansettelsesperiode),
            tom = getTom(arbeidsforholdDto.ansettelsesperiode),
            navArbeidsforholdId = arbeidsforholdDto.navArbeidsforholdId?.toString()
    )

    private fun orgnummerHvisOrganisasjon(arbeidsgiver: ArbeidsgiverDto): String? =
            arbeidsgiver.takeIf { it.type == "Organisasjon" }
                    ?.organisasjonsnummer

    private fun getFom(ansettelsesPeriode: AnsettelsesperiodeDto?): LocalDate? =
            ansettelsesPeriode
                    ?.periode
                    ?.fom
                    ?.let(LocalDate::parse)

    private fun getTom(periode: AnsettelsesperiodeDto?): LocalDate? {
        return periode
                ?.periode
                ?.tom
                ?.let(LocalDate::parse)
    }

    private fun List<ArbeidsavtaleDto>.sisteArbeidsavtale(): ArbeidsavtaleDto? {

        val gjeldendeArbeidsavtale: ArbeidsavtaleDto? = this
                .filter { it.gyldighetsperiode.tom == null }
                .maxByOrNull { LocalDate.parse(it.gyldighetsperiode.fom) }

        val senesteAvsluttedeArbeidsavtale: ArbeidsavtaleDto? = this
                .filter { it.gyldighetsperiode.tom != null }
                .maxByOrNull { LocalDate.parse(it.gyldighetsperiode.tom) }

        return gjeldendeArbeidsavtale ?: senesteAvsluttedeArbeidsavtale
    }
}