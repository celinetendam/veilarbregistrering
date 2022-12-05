package no.nav.fo.veilarbregistrering.registrering.sykmeldt

import no.nav.fo.veilarbregistrering.bruker.Bruker
import no.nav.fo.veilarbregistrering.log.secureLogger
import no.nav.fo.veilarbregistrering.metrics.Events
import no.nav.fo.veilarbregistrering.metrics.MetricsService
import no.nav.fo.veilarbregistrering.oppfolging.OppfolgingGateway
import no.nav.fo.veilarbregistrering.registrering.BrukerRegistreringType.SYKMELDT
import no.nav.fo.veilarbregistrering.registrering.Tilstandsfeil
import no.nav.fo.veilarbregistrering.registrering.bruker.BrukerTilstandService
import no.nav.fo.veilarbregistrering.registrering.veileder.NavVeileder
import no.nav.fo.veilarbregistrering.registrering.bruker.RegistreringType
import no.nav.fo.veilarbregistrering.registrering.veileder.ManuellRegistrering
import no.nav.fo.veilarbregistrering.registrering.veileder.ManuellRegistreringRepository
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional

open class SykmeldtRegistreringService(
    private val brukerTilstandService: BrukerTilstandService,
    private val oppfolgingGateway: OppfolgingGateway,
    private val sykmeldtRegistreringRepository: SykmeldtRegistreringRepository,
    private val manuellRegistreringRepository: ManuellRegistreringRepository,
    private val metricsService: MetricsService
) {
    @Transactional
    open fun registrerSykmeldt(sykmeldtRegistrering: SykmeldtRegistrering, bruker: Bruker, navVeileder: NavVeileder?): Long {
        sjekkAtBrukerKanRegistreres(bruker)
        oppfolgingGateway.aktiverSykmeldt(bruker.gjeldendeFoedselsnummer, sykmeldtRegistrering.besvarelse)
        val id = sykmeldtRegistreringRepository.lagreSykmeldtBruker(sykmeldtRegistrering, bruker)
        lagreManuellRegistrering(id, navVeileder)
        registrerOverfortStatistikk(navVeileder)
        LOG.info("Sykmeldtregistrering gjennomført med data {}", sykmeldtRegistrering)
        metricsService.registrer(Events.REGISTRERING_FULLFORING_REGISTRERINGSTYPE, RegistreringType.SYKMELDT_REGISTRERING)
        metricsService.registrer(Events.SYKMELDT_BESVARELSE_EVENT)
        return id
    }

    private fun sjekkAtBrukerKanRegistreres(bruker: Bruker) {
        val brukersTilstand = brukerTilstandService.hentBrukersTilstand(bruker)
        if (brukersTilstand.ikkeErSykemeldtRegistrering()) {
            secureLogger.warn("Bruker, ${bruker.aktorId}, kan ikke sykemeldtregistreres fordi utledet registreringstype er ${brukersTilstand.registreringstype}")
            metricsService.registrer(Events.REGISTRERING_TILSTANDSFEIL, Tilstandsfeil.IKKE_SYKEMELDT_REGISTRERING)
            throw RuntimeException(
                    "Brukeren kan ikke sykemeldtregistreres fordi utledet registreringstype er ${brukersTilstand.registreringstype}"
            )
        }
    }

    private fun lagreManuellRegistrering(id: Long, veileder: NavVeileder?) {
        if (veileder == null) return
        val manuellRegistrering = ManuellRegistrering(id, SYKMELDT, veileder.veilederIdent, veileder.enhetsId)
        manuellRegistreringRepository.lagreManuellRegistrering(manuellRegistrering)
    }

    private fun registrerOverfortStatistikk(veileder: NavVeileder?) {
        if (veileder == null) return
        metricsService.registrer(Events.MANUELL_REGISTRERING_EVENT, SYKMELDT)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(SykmeldtRegistreringService::class.java)
    }
}