package com.besp.pki.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

@Service
public class KeyStoreService {

    public record EntryRef(String path, String alias) {}

    public EntryRef storeEntryPerCert(String baseDir, String alias, PrivateKey privateKey, char[] entryPass, X509Certificate[] chain) {
        try {
            File dir = new File(baseDir);
            if (!dir.exists()) dir.mkdirs();
            File ksFile = new File(dir, alias + ".p12");

            KeyStore ks = KeyStore.getInstance("PKCS12");
            if (ksFile.exists()) {
                ks.load(Files.newInputStream(ksFile.toPath()), entryPass); // pokušaj da učita sa istom lozinkom
            } else {
                ks.load(null, null);
            }
            KeyStore.ProtectionParameter prot = new KeyStore.PasswordProtection(entryPass);
            KeyStore.PrivateKeyEntry entry = new KeyStore.PrivateKeyEntry(privateKey, chain);
            ks.setEntry(alias, entry, prot);
            try (FileOutputStream fos = new FileOutputStream(ksFile)) {
                ks.store(fos, entryPass); // koristimo istu lozinku i za fajl
            }
            return new EntryRef(ksFile.getPath(), alias);
        } catch (Exception e) {
            throw new RuntimeException("KeyStore storeEntryPerCert failed", e);
        }
    }

    public PrivateKey readPrivateKey(String ksPath, String alias, char[] entryPass) {
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(Files.newInputStream(new File(ksPath).toPath()), entryPass);
            Key k = ks.getKey(alias, entryPass);
            if (k instanceof PrivateKey pk) return pk;
            throw new RuntimeException("No private key for alias " + alias);
        } catch (Exception e) {
            throw new RuntimeException("KeyStore readPrivateKey failed", e);
        }
    }

    public X509Certificate readCertificate(String ksPath, String alias, char[] entryPass) {
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(Files.newInputStream(new File(ksPath).toPath()), entryPass);
            Certificate c = ks.getCertificate(alias);
            return (X509Certificate) c;
        } catch (Exception e) {
            throw new RuntimeException("KeyStore readCertificate failed", e);
        }
    }

    public X509Certificate[] readChain(String ksPath, String alias, char[] entryPass) {
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(Files.newInputStream(new File(ksPath).toPath()), entryPass);
            Certificate[] ch = ks.getCertificateChain(alias);
            if (ch == null) return null;
            X509Certificate[] arr = new X509Certificate[ch.length];
            for (int i=0;i<ch.length;i++) arr[i] = (X509Certificate) ch[i];
            return arr;
        } catch (Exception e) {
            throw new RuntimeException("KeyStore readChain failed", e);
        }
    }
}
