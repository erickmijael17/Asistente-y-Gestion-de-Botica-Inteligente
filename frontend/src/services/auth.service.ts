import api from '../api/axios';

export interface LoginRequest {
  username: string;
  password?: string; // En el sistema real debería enviarse encriptado o manejado seguro, pero para el backend que creamos enviamos texto plano y bcrypt se encarga en backend
}

export interface RegisterRequest {
  username: string;
  password?: string;
  nombres: string;
  apellidos: string;
  roles: string;
}

export interface AuthResponse {
  token: string;
  type: string;
  username: string;
  roles: string;
}

export const authService = {
  login: async (data: LoginRequest): Promise<AuthResponse> => {
    const response = await api.post<AuthResponse>('/auth/login', data, {
      baseURL: import.meta.env.VITE_AUTH_URL
    });
    return response.data;
  },

  register: async (data: RegisterRequest): Promise<AuthResponse> => {
    const response = await api.post<AuthResponse>('/auth/register', data, {
      baseURL: import.meta.env.VITE_AUTH_URL
    });
    return response.data;
  },
};
