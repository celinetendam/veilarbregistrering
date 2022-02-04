package no.nav.fo.veilarbregistrering.kafka;

import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.fo.veilarbregistrering.registrering.publisering.ArbeidssokerRegistrertInternalEvent;
import no.nav.fo.veilarbregistrering.registrering.publisering.ArbeidssokerRegistrertProducer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

import static no.nav.common.log.MDCConstants.MDC_CALL_ID;
import static no.nav.fo.veilarbregistrering.kafka.ArbeidssokerRegistrertMapper.map;
import static no.nav.fo.veilarbregistrering.log.CallId.getCorrelationIdAsBytes;

class ArbeidssokerRegistrertKafkaProducer implements ArbeidssokerRegistrertProducer {

    private static final Logger LOG = LoggerFactory.getLogger(ArbeidssokerRegistrertKafkaProducer.class);

    private final KafkaProducer producer;
    private final String topic;

    ArbeidssokerRegistrertKafkaProducer(KafkaProducer kafkaProducer, String topic) {
        this.producer = kafkaProducer;
        this.topic = topic;
    }

    @Override
    public boolean publiserArbeidssokerRegistrert(
            ArbeidssokerRegistrertInternalEvent arbeidssokerRegistrertInternalEvent) {

        try {
            ArbeidssokerRegistrertEvent arbeidssokerRegistrertEvent = map(arbeidssokerRegistrertInternalEvent);
            ProducerRecord<String, ArbeidssokerRegistrertEvent> record = new ProducerRecord<>(topic, arbeidssokerRegistrertInternalEvent.getAktorId().getAktorId(), arbeidssokerRegistrertEvent);
            record.headers().add(new RecordHeader(MDC_CALL_ID, getCorrelationIdAsBytes()));
            AtomicBoolean resultat = new AtomicBoolean(false);
            producer.send(record, (recordMetadata, e) -> {
                if (e != null) {
                    LOG.error(String.format("ArbeidssokerRegistrertEvent publisert på topic, %s", topic), e);
                    resultat.set(false);
                } else {
                    LOG.info("ArbeidssokerRegistrertEvent publisert: {}", recordMetadata);
                    resultat.set(true);
                }
            }).get();
            return resultat.get();

        } catch (Exception e) {
            LOG.error("Sending av arbeidssokerRegistrertEvent til Kafka feilet", e);
            return false;
        }
    }
}
