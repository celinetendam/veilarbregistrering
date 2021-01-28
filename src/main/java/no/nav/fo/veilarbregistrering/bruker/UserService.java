package no.nav.fo.veilarbregistrering.bruker;

import no.bekk.bekkopen.person.FodselsnummerValidator;
import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbregistrering.bruker.feil.ManglendeBrukerInfoException;
import org.springframework.stereotype.Service;

import static no.nav.common.auth.context.AuthContextHolder.getSubject;
import static no.nav.fo.veilarbregistrering.config.RequestContext.servletRequest;

@Service
public class UserService {

    private final PdlOppslagGateway pdlOppslagGateway;

    public UserService(PdlOppslagGateway pdlOppslagGateway) {
        this.pdlOppslagGateway = pdlOppslagGateway;
    }

    public Bruker finnBrukerGjennomPdl() {
        Foedselsnummer fnr = hentFnrFraUrlEllerToken();
        return finnBrukerGjennomPdl(fnr);
    }

    public Bruker finnBrukerGjennomPdl(Foedselsnummer fnr) {
        return map(pdlOppslagGateway.hentIdenter(fnr));
    }

    public Bruker hentBruker(AktorId aktorId) {
        return map(pdlOppslagGateway.hentIdenter(aktorId));
    }

    private static Bruker map(Identer identer) {
        return Bruker.of(
                identer.finnGjeldendeFnr(),
                identer.finnGjeldendeAktorId(),
                identer.finnHistoriskeFoedselsnummer());
    }

    private Foedselsnummer hentFnrFraUrlEllerToken() {

        String fnr = getFnrFromUrl();

        if (fnr == null) {
            fnr = getFnr();
        }

        if (!FodselsnummerValidator.isValid(fnr)) {
            throw new ManglendeBrukerInfoException("Fødselsnummer ikke gyldig.");
        }

        return Foedselsnummer.of(fnr);
    }

    public String getFnrFromUrl() {
        return servletRequest().getParameter("fnr");
    }

    private String getFnr() {
        return getSubject().orElseThrow(IllegalArgumentException::new);
    }

    public String getEnhetIdFromUrlOrThrow() {
        final String enhetId = servletRequest().getParameter("enhetId");

        if (enhetId == null) {
            throw new ManglendeBrukerInfoException("Mangler enhetId");
        }

        return enhetId;
    }
}
