export type CertificateStatus = 'ACTIVE' | 'REVOKED' | 'EXPIRED';
export type CertificateType = 'ROOT' | 'INTERMEDIATE' | 'EE' | 'CA';

export interface CertificateListItem {
  id: number;
  serialNumber: string;
  subjectDn: string;
  issuerDn: string;
  issuerId?: number | null;
  ownerUserId?: number | null;
  notBefore: string;
  notAfter: string;
  ca: boolean;
  pathLenConstraint?: number | null;
  status: CertificateStatus;
  type: CertificateType;
  keySize?: number | null;
  fingerprintSha256: string;
  revocationReason?: string | null;
  revocationDate?: string | null;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface RevokeRequest {
  reason: 'UNSPECIFIED' | 'KEY_COMPROMISED' | 'CA_COMPROMISED' | 'AFFILIATION_CHANGED' |
          'SUPERSEDED' | 'CESSATION_OF_OPERATION' | 'CERTIFICATE_HOLD' | 'PRIVILEGE_WITHDRAWN' | 'REMOVE_FROM_CRL' | 'AA_COMPROMISED';
}
