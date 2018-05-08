package no.nav.fo.veilarbregistrering.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;

@Accessors(chain = true)
@Data
public class OppfolgingStatus {
    private LocalDate inaktiveringsdato;
    private boolean underOppfolging;
}
