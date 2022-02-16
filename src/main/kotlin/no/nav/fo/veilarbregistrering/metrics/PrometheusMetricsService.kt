package no.nav.fo.veilarbregistrering.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import no.nav.fo.veilarbregistrering.registrering.formidling.Status
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * PrometheusMetricsService fungerer som en abstraksjon mot Prometheus, og tilbyr funksjoner
 * for å ...
 *
 * Prometheus benytter en pull-modell, hvor appen tilbyr et endepunkt for å hente data. Se application.yml.
 */
class PrometheusMetricsService(private val meterRegistry: MeterRegistry) {

    fun rapporterRegistreringStatusAntall(antallPerStatus: Map<Status, Int>) {
        antallPerStatus.forEach {
            val registrertAntall = statusVerdier.computeIfAbsent(it.key) { key ->
                val atomiskAntall = AtomicInteger()
                meterRegistry.gauge(
                        "veilarbregistrering_registrert_status",
                        listOf(Tag.of("status", key.name)),
                        atomiskAntall)
                atomiskAntall
            }
            registrertAntall.set(it.value)
        }
    }
    
    private val statusVerdier: MutableMap<Status, AtomicInteger> = EnumMap(Status::class.java)

    fun registrer(event: Event, vararg tags: Tag) {
        meterRegistry.counter(event.key, tags.asIterable()).increment()
    }

    fun registrer(event: Event) {
        meterRegistry.counter(event.key).increment()
    }

    fun registrer(event: Event, vararg metrikk: Metric) {
        val tags = metrikk.map { Tag.of(it.fieldName(), it.value().toString()) }.toTypedArray()
        registrer(event, *tags)
    }
}
