package no.nav.fo.veilarbregistrering.registrering.resources;

import no.nav.fo.veilarbregistrering.registrering.bruker.RegistreringTilstandService;
import no.nav.fo.veilarbregistrering.registrering.bruker.Status;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class InternalRegistreringStatusServlet extends HttpServlet {

    private final RegistreringTilstandService registreringTilstandService;

    public InternalRegistreringStatusServlet(RegistreringTilstandService registreringTilstandService) {
        this.registreringTilstandService = registreringTilstandService;
    }

    public static final String PATH = "/internal/status";

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Status status = Status.parse(req.getParameter("status"));
        String id = req.getParameter("id");

        registreringTilstandService.oppdaterRegistreringTilstand(RegistreringTilstandDto.of(id, status));
    }
}
