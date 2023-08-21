package ru.headbridge.certmetrics.metrics;

import io.micrometer.common.lang.NonNull;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@AllArgsConstructor
public class CertificateMetrics implements MeterBinder {

    @Override
    @Timed
    public void bindTo(@NonNull MeterRegistry registry) {
        List<CertificateInfo> certificates = loadCertificates();
        for (CertificateInfo cert : certificates) {
            AtomicInteger daysLeft = new AtomicInteger(cert.daysLeft());
            Gauge.builder("certificate.days.left", daysLeft, AtomicInteger::get)
                    .tags(Tags.of("certificate", cert.name()))
                    .description("Days left")
                    .register(registry);
        }
    }

    private List<CertificateInfo> loadCertificates() {
        try {
            KeyStore keyStore = loadKeyStore(System.getProperty("java.home") + "/lib/security/cacerts", "changeit");
            return extractCertificateInfo(keyStore);
        } catch (Exception e) {
            throw new RuntimeException(e);
//            e.fillInStackTrace();
//            return Collections.emptyList();
        }
    }

    private KeyStore loadKeyStore(String keystorePath, String keystorePassword) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            keyStore.load(fis, keystorePassword.toCharArray());
        }
        return keyStore;
    }

    private List<CertificateInfo> extractCertificateInfo(KeyStore keyStore) throws Exception {
        List<CertificateInfo> certificateInfoList = new ArrayList<>();
        Enumeration<String> aliases = keyStore.aliases();

        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            Certificate cert = keyStore.getCertificate(alias);
            if (cert instanceof X509Certificate x509Cert) {
                String name = x509Cert.getSubjectX500Principal().getName();
                long daysLeft = getDaysLeft(x509Cert.getNotAfter());
                certificateInfoList.add(new CertificateInfo(name, (int) daysLeft));
            }
        }
        return certificateInfoList;
    }

    private long getDaysLeft(Date expirationDate) {
        long diffInMillies = expirationDate.getTime() - System.currentTimeMillis();
        return TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
    }
}
