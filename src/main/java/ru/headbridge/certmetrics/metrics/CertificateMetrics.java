package ru.headbridge.certmetrics.metrics;

import io.micrometer.common.lang.NonNull;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class CertificateMetrics implements MeterBinder {

    private final CertificateLoader certLoader;
    private final MeterRegistry registry;

    @Override
    public void bindTo(@NonNull MeterRegistry registry) {
        refreshMetrics();
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void refreshMetrics() {
        registry.getMeters().stream()
                .filter(meter -> "certificate.daysLeft".equals(meter.getId().getName()))
                .findFirst()
                .ifPresent(registry::remove);

        Integer size = certLoader.loadCertificates();
        List<Tag> tags = getTags(certLoader.getCertInfos());

        Gauge.builder("certificate.daysLeft", () -> size)
                .description("Days left")
                .baseUnit("days")
                .tags(tags)
                .register(registry);
    }

    private List<Tag> getTags(List<CertificateInfo> certificateInfos) {
        return certificateInfos.stream()
                .map(cert -> Tag.of(cert.name(), cert.daysLeft() + ""))
                .collect(Collectors.toList());
    }
}
