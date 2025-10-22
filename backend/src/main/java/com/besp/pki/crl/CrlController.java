package com.besp.pki.crl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CrlController {

    private final CrlService crl;

    @GetMapping(value="/crl/{issuerId}.crl", produces="application/pkix-crl")
    public ResponseEntity<byte[]> getCrl(@PathVariable long issuerId) {
        byte[] der = crl.currentCrl(issuerId);
        if (der == null) der = crl.rebuild(issuerId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/pkix-crl"))
                .body(der);
    }
}
