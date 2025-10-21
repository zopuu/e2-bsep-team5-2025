export interface CsrData {
  commonName: string;
  organization: string;
  organizationalUnit: string;
  locality: string;
  state: string;
  country: string;
  emailAddress: string;
  publicKey: any; // PublicKey object
  subjectAlternativeNames: string[];
  keyUsage: string;
  extendedKeyUsage: string;
}

export interface CsrUploadRequest {
  csrContent: string;
}

export interface CsrUploadResponse {
  success: boolean;
  message: string;
  csrData?: CsrData;
}

export interface CaIssuerDto {
  id: number;
  subjectDn: string;
  issuerDn: string;
  serialNumber: string;
  ca: boolean;
  notBefore: string;
  notAfter: string;
}

export interface CsrCertificateRequest {
  csrData: CsrData;
  caCertificateId: number;
  validityDays: number;
}
