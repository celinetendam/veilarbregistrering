package no.nav.fo.veilarbregistrering.oppgave;

import no.nav.fo.veilarbregistrering.bruker.Bruker;
import no.nav.fo.veilarbregistrering.metrics.MetricsService;
import no.nav.fo.veilarbregistrering.orgenhet.Enhetnr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static no.nav.fo.veilarbregistrering.metrics.Events.OPPGAVE_ALLEREDE_OPPRETTET_EVENT;
import static no.nav.fo.veilarbregistrering.metrics.Events.OPPGAVE_OPPRETTET_EVENT;
import static no.nav.fo.veilarbregistrering.oppgave.OppgavePredicates.oppgaveAvType;
import static no.nav.fo.veilarbregistrering.oppgave.OppgavePredicates.oppgaveOpprettetForMindreEnnToArbeidsdagerSiden;

public class OppgaveService {

    private final Logger LOG = LoggerFactory.getLogger(OppgaveService.class);

    private final MetricsService metricsService;
    private final OppgaveGateway oppgaveGateway;
    private final OppgaveRepository oppgaveRepository;
    private final OppgaveRouter oppgaveRouter;
    private final KontaktBrukerHenvendelseProducer kontaktBrukerHenvendelseProducer;

    public OppgaveService(
            OppgaveGateway oppgaveGateway,
            OppgaveRepository oppgaveRepository,
            OppgaveRouter oppgaveRouter,
            KontaktBrukerHenvendelseProducer kontaktBrukerHenvendelseProducer,
            MetricsService metricsService) {

        this.metricsService = metricsService;
        this.oppgaveGateway = oppgaveGateway;
        this.oppgaveRepository = oppgaveRepository;
        this.oppgaveRouter = oppgaveRouter;
        this.kontaktBrukerHenvendelseProducer = kontaktBrukerHenvendelseProducer;
    }

    public OppgaveResponse opprettOppgave(Bruker bruker, OppgaveType oppgaveType) {
        validerNyOppgaveMotAktive(bruker, oppgaveType);

        kontaktBrukerHenvendelseProducer.publiserHenvendelse(bruker.getAktorId(), oppgaveType);

        Optional<Enhetnr> enhetsnr = oppgaveRouter.hentEnhetsnummerFor(bruker);

        Oppgave oppgave = Oppgave.opprettOppgave(bruker.getAktorId(),
                enhetsnr.orElse(null),
                oppgaveType,
                idag());

        OppgaveResponse oppgaveResponse = oppgaveGateway.opprett(oppgave);

        LOG.info("Oppgave (type:{}) ble opprettet med id: {} og ble tildelt enhet: {}",
                oppgaveType, oppgaveResponse.getId(), oppgaveResponse.getTildeltEnhetsnr());

        oppgaveRepository.opprettOppgave(bruker.getAktorId(), oppgaveType, oppgaveResponse.getId());

        metricsService.reportSimple(OPPGAVE_OPPRETTET_EVENT, TildeltEnhetsnr.of(oppgaveResponse.getTildeltEnhetsnr()), oppgaveType);

        return oppgaveResponse;
    }

    private void validerNyOppgaveMotAktive(Bruker bruker, OppgaveType oppgaveType) {
        List<OppgaveImpl> oppgaver = oppgaveRepository.hentOppgaverFor(bruker.getAktorId());
        Optional<OppgaveImpl> muligOppgave = oppgaver.stream()
                .filter(oppgaveAvType(oppgaveType))
                .filter(oppgaveOpprettetForMindreEnnToArbeidsdagerSiden(idag()))
                .findFirst();

        muligOppgave.ifPresent(oppgave -> {
            metricsService.reportSimple(OPPGAVE_ALLEREDE_OPPRETTET_EVENT, oppgave.getOpprettet(), oppgaveType);

            throw new OppgaveAlleredeOpprettet(
                    String.format(
                            "Fant en oppgave av samme type %s som ble opprettet %s - %s timer siden.",
                            oppgave.getOppgavetype(),
                            oppgave.getOpprettet().tidspunkt(),
                            oppgave.getOpprettet().antallTimerSiden())
            );
        });
    }

    /**
     * Protected metode for å kunne overskrive ifm. test.
     */
    protected LocalDate idag() {
        return LocalDate.now();
    }

}
