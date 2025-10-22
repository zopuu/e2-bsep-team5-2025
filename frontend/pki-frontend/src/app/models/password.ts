export interface PasswordEntry {
  id: number;
  siteName: string;
  username: string;
  encryptedPassword: string;
  notes?: string;
  createdAt: string;
  updatedAt: string;
  certificateId: number;
  certificateSubject: string;
}

export interface PasswordEntryRequest {
  siteName: string;
  username: string;
  password: string;
  notes?: string;
  certificateId: number;
}

export interface DecryptRequest {
  encryptedPassword: string;
  privateKeyPem: string;
}

export interface DecryptResponse {
  decryptedPassword: string;
}
