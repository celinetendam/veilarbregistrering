package no.nav.fo.veilarbregistrering.arbeidssoker.formidlingsgruppe

import no.nav.common.featuretoggle.UnleashClient
import no.nav.fo.veilarbregistrering.aktorIdCache.AktorIdCacheService
import no.nav.fo.veilarbregistrering.arbeidssoker.Arbeidssoker
import no.nav.fo.veilarbregistrering.arbeidssoker.perioder.ArbeidssokerperiodeAvsluttetProducer
import no.nav.fo.veilarbregistrering.arbeidssoker.perioder.ArbeidssokerperiodeAvsluttetService
import no.nav.fo.veilarbregistrering.arbeidssoker.perioder.Arbeidssokerperioder
import no.nav.fo.veilarbregistrering.arbeidssoker.perioder.PopulerArbeidssokerperioderService
import no.nav.fo.veilarbregistrering.bruker.Foedselsnummer
import no.nav.fo.veilarbregistrering.log.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class FormidlingsgruppeMottakService(
    private val formidlingsgruppeRepository: FormidlingsgruppeRepository,
    private val arbeidssokerperiodeAvsluttetService: ArbeidssokerperiodeAvsluttetService,
    private val aktorIdCacheService: AktorIdCacheService,
    private val populerArbeidssokerperioderService: PopulerArbeidssokerperioderService,
    private val arbeidssokerperiodeAvsluttetProducer: ArbeidssokerperiodeAvsluttetProducer
) {

    @Transactional
    fun behandle(formidlingsgruppeEndretEvent: FormidlingsgruppeEndretEvent) {

        if (formidlingsgruppeEndretEvent.formidlingsgruppeEndret.isBefore(LocalDateTime.parse("2010-01-01T00:00:00"))
            && formidlingsgruppeEndretEvent.formidlingsgruppe.kode != "ARBS") {
            logger.warn(
                "Fikk formidlingsgruppeendring fra før 2010 som ikke har formidlingsgruppe ARBS, " +
                        "formidlingsgruppe: ${formidlingsgruppeEndretEvent.formidlingsgruppe.kode}, " +
                        "dato: ${formidlingsgruppeEndretEvent.formidlingsgruppeEndret}) ")
        }

        if (formidlingsgruppeEndretEvent.operation != Operation.UPDATE) {
            logger.info("Forkaster melding som ikke er UPDATE, men lagrer for ettertid - $formidlingsgruppeEndretEvent")
            if (formidlingsgruppeEndretEvent.formidlingsgruppe.kode != "ISERV") {
                logger.error("Mottok en INSERT-melding med formidlingsgruppe ${formidlingsgruppeEndretEvent.formidlingsgruppe} - vi skal kun få INSERT med ISERV")
            }
            formidlingsgruppeRepository.lagre(formidlingsgruppeEndretEvent)
            return
        }

        val eksisterendeArbeidssokerperioderLokalt = hentArbeidssøkerperioder(formidlingsgruppeEndretEvent)
        val arbeidssøker = hentArbeidssøker(formidlingsgruppeEndretEvent.foedselsnummer)

        formidlingsgruppeRepository.lagre(formidlingsgruppeEndretEvent)

        try {
            aktorIdCacheService.hentAktorIdFraPDLHvisIkkeFinnes(formidlingsgruppeEndretEvent.foedselsnummer, true)
        } catch (e: Exception) {
            logger.warn("Klarte ikke populere aktørid-cache for innkommende formidlingsgruppe", e)
        }

        arbeidssokerperiodeAvsluttetService.behandleAvslutningAvArbeidssokerperiode(
            formidlingsgruppeEndretEvent,
            eksisterendeArbeidssokerperioderLokalt
        )

        behandle(arbeidssøker, formidlingsgruppeEndretEvent)
    }

    private fun hentArbeidssøkerperioder(formidlingsgruppeEndretEvent: FormidlingsgruppeEndretEvent): Arbeidssokerperioder {
        val eksisterendeFormidlingsgruppeEndretEvents =
            formidlingsgruppeRepository.finnFormidlingsgruppeEndretEventFor(
                listOf(formidlingsgruppeEndretEvent.foedselsnummer)
            )

        return ArbeidssokerperioderMapper.map(eksisterendeFormidlingsgruppeEndretEvents)
    }

    private fun hentArbeidssøker(foedselsnummer: Foedselsnummer): Arbeidssoker? {
        return try {
            val arbeidssøker = populerArbeidssokerperioderService.hentArbeidssøker(foedselsnummer)
            arbeidssøker.add(arbeidssokerperiodeAvsluttetProducer)
            arbeidssøker
        } catch (e: RuntimeException) {
            logger.error("Henting av arbeidssøker feilet", e)
            null
        }
    }

    private fun behandle(arbeidssøker: Arbeidssoker?, formidlingsgruppeEndretEvent: FormidlingsgruppeEndretEvent) {
        if (arbeidssøker == null) return
        try {
            logger.info("Behandler mottak av $formidlingsgruppeEndretEvent")
            arbeidssøker.behandle(formidlingsgruppeEndretEvent)
            arbeidssøker.remove(arbeidssokerperiodeAvsluttetProducer)
        } catch (e: RuntimeException) {
            logger.error("Behandling av formidlingsgruppeEndretEvent feilet", e)
        }
    }
}
