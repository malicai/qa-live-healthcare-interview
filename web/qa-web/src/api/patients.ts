import { request } from './http';
import type { Patient } from '../store';

export interface PatientResponse {
  patient: Patient;
}

export interface PatientRegisterPayload {
  username: string;
  password: string;
  name: string;
  birthday: string;
  phone?: string;
  gender?: string;
}

export function registerPatient(payload: PatientRegisterPayload): Promise<PatientResponse> {
  return request<PatientResponse>('/api/v1/patients/register', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function loginPatient(username: string, password: string): Promise<PatientResponse> {
  return request<PatientResponse>('/api/v1/patients/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });
}
