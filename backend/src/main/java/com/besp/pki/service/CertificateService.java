package com.besp.pki.service;

import com.besp.pki.dto.RootCaRequest;
import com.besp.pki.entity.CertificateRecord;
import com.besp.pki.repository.CertificateRecordRepository;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Security;

@Service
public class CertificateService {

    private final com.besp.pki.repository.CertificateRecordRepository repo;

    @Value("${pki.keystore-dir:./data/keystores}")
    private String keystoreDir;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }
    public CertificateService(CertificateRecordRepository repo){
        this.repo = repo;
    }
    public CertificateRecord createRootCa(RootCaRequest req, String adminEmail) throws Exception {
        // TODO
        return null;
    }
}
