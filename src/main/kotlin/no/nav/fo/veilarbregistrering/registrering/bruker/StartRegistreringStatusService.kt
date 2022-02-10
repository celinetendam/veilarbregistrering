package no.nav.fo.veilarbregistrering.registrering.bruker

import no.nav.fo.veilarbregistrering.arbeidsforhold.ArbeidsforholdGateway
import no.nav.fo.veilarbregistrering.bruker.Bruker
import no.nav.fo.veilarbregistrering.bruker.GeografiskTilknytning
import no.nav.fo.veilarbregistrering.bruker.PdlOppslagGateway
import no.nav.fo.veilarbregistrering.metrics.PrometheusMetricsService
import no.nav.fo.veilarbregistrering.registrering.bruker.StartRegistreringStatusMetrikker.rapporterRegistreringsstatus
import no.nav.fo.veilarbregistrering.registrering.bruker.resources.StartRegistreringStatusDto
import no.nav.fo.veilarbregistrering.registrering.bruker.resources.StartRegistreringStatusDtoMapper.map
import org.slf4j.LoggerFactory
import java.time.LocalDate

class StartRegistreringStatusService(
    private val arbeidsforholdGateway: ArbeidsforholdGateway,
    private val brukerTilstandService: BrukerTilstandService,
    private val pdlOppslagGateway: PdlOppslagGateway,
    private val prometheusMetricsService: PrometheusMetricsService
) {
    fun hentStartRegistreringStatus(bruker: Bruker): StartRegistreringStatusDto {
        val brukersTilstand = brukerTilstandService.hentBrukersTilstand(bruker)
        val muligGeografiskTilknytning = hentGeografiskTilknytning(bruker)

        muligGeografiskTilknytning.apply {
            LOG.info(
                "Bruker {} startet registrering med geografisk tilknytning [BrukersTilstand], [GeografiskTilknytning] [{}] [{}]",
                bruker.aktorId,
                brukersTilstand,
                this
            )
        }

        val registreringType = brukersTilstand.registreringstype
        var oppfyllerBetingelseOmArbeidserfaring: Boolean? = null
        if (RegistreringType.ORDINAER_REGISTRERING == registreringType) {
            oppfyllerBetingelseOmArbeidserfaring =
                arbeidsforholdGateway.hentArbeidsforhold(bruker.gjeldendeFoedselsnummer)
                    .harJobbetSammenhengendeSeksAvTolvSisteManeder(LocalDate.now())
        }
        val startRegistreringStatus = map(
            brukersTilstand,
            muligGeografiskTilknytning,
            oppfyllerBetingelseOmArbeidserfaring,
            bruker.gjeldendeFoedselsnummer.alder(LocalDate.now())
        )
        LOG.info("Startreg.status for {}: {}", bruker.aktorId, startRegistreringStatus)
        rapporterRegistreringsstatus(prometheusMetricsService, startRegistreringStatus)
        return startRegistreringStatus
    }

    private fun hentGeografiskTilknytning(bruker: Bruker): GeografiskTilknytning? {
        val t1 = System.currentTimeMillis()

        val geografiskTilknytning = try {
            pdlOppslagGateway.hentGeografiskTilknytning(bruker.aktorId)?.also {
                LOG.info("Henting av geografisk tilknytning tok {} ms.", System.currentTimeMillis() - t1)
            }
        } catch (e: RuntimeException) {
            LOG.warn("Hent geografisk tilknytning fra PDL feilet. Skal ikke påvirke annen bruk.", e)
            null
        }
        return geografiskTilknytning
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(StartRegistreringStatusService::class.java)
    }
}