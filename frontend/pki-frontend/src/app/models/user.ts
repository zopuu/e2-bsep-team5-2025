export type UserRole = 'ADMIN' | 'CA_USER' | 'REGULAR_USER' | string;

export interface UserDto {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  organization: string;
  role: UserRole;
  enabled: boolean;
  emailVerified: boolean;
  createdAt?: string;
  updatedAt?: string;
  lastLogin?: string;
}

export interface CreateCaUserRequest {
  email: string;
  firstName: string;
  lastName: string;
  organization: string;
}
