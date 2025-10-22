package com.besp.pki.ocsp;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class OcspController {

    private final OcspService ocsp;

    @PostMapping(value="/ocsp", consumes="application/ocsp-request", produces="application/ocsp-response")
    public ResponseEntity<byte[]> handle(@RequestBody byte[] req) {
        byte[] resp = ocsp.handle(req);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/ocsp-response"))
                .body(resp);
    }
}
