package no.nav.fo.veilarbregistrering.metrics;

import no.nav.metrics.MetricsFactory;

import java.util.Arrays;

public class Metrics {

    public static void report(Event event, Metric... metric) {
        no.nav.metrics.Event metricsEvent = MetricsFactory.createEvent(event.name);
        Arrays.stream(metric).forEach(m -> metricsEvent.addTagToReport(m.fieldName(), m.value()));
        metricsEvent.report();
    }

    public enum Event {
        OPPGAVE_OPPRETTET_EVENT("arbeid.registrert.oppgave");

        private final String name;

        Event(String name) {
            this.name = name;
        }
    }
}
