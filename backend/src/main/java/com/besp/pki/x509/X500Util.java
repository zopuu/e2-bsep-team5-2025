package com.besp.pki.x509;

import com.besp.pki.dto.NameDto;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;

public class X500Util {
    public static X500Name fromDto(NameDto d) {
        X500NameBuilder b = new X500NameBuilder(BCStyle.INSTANCE);
        if (nz(d.commonName())) b.addRDN(BCStyle.CN, d.commonName());
        if (nz(d.organization())) b.addRDN(BCStyle.O, d.organization());
        if (nz(d.organizationalUnit())) b.addRDN(BCStyle.OU, d.organizationalUnit());
        if (nz(d.locality())) b.addRDN(BCStyle.L, d.locality());
        if (nz(d.state())) b.addRDN(BCStyle.ST, d.state());
        if (nz(d.country())) b.addRDN(BCStyle.C, d.country());
        return b.build();
    }
    private static boolean nz(String s){ return s!=null && !s.isBlank(); }
}
