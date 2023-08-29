package ru.headbridge.certmetrics.metrics;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
@Getter
public class CertificateLoader {
    private List<CertificateInfo> certInfos = Collections.emptyList();

    public Integer loadCertificates() {
        try {
            KeyStore keyStore = loadKeyStore(System.getProperty("java.home") + "/lib/security/cacerts", "changeit");
            this.certInfos = extractCertificateInfo(keyStore);
            return this.certInfos.size();
        } catch (Exception e) {
            e.fillInStackTrace();
        }
        return 0;
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
