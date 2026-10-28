import { request } from './http';
import type { Doctor } from '../store';

export interface DoctorPage {
  items: Doctor[];
  total: number;
  page: number;
  size: number;
}

export interface DoctorListParams {
  page?: number;
  size?: number;
  isActive?: boolean;
}

export interface DoctorLoginResponse {
  doctor: Doctor;
}

export function fetchDoctors(params: DoctorListParams = {}): Promise<DoctorPage> {
  const query = new URLSearchParams();
  query.set('page', String(params.page ?? 1));
  query.set('size', String(params.size ?? 20));

  if (params.isActive !== undefined) {
    query.set('isActive', String(params.isActive));
  }

  return request<DoctorPage>(`/api/v1/doctors?${query.toString()}`);
}

export function loginDoctor(username: string, password: string): Promise<DoctorLoginResponse> {
  return request<DoctorLoginResponse>('/api/v1/doctors/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });
}
