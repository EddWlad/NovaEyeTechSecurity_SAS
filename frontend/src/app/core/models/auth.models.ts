import { Role } from './enums';

export interface SessionUser {
  id: string;
  email: string;
  fullName: string;
  role: Role;
  phone?: string | null;
  avatarDataUrl?: string | null;
  active: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  access_token: string;
  user: SessionUser;
}
