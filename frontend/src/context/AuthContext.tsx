import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { jwtDecode } from 'jwt-decode';
import type { RolUsuario, UsuarioSesion } from '../types/domain.types';

interface AuthContextType {
  usuario: UsuarioSesion | null;
  token: string | null;
  iniciarSesion: (token: string, userId: number, username: string, roles: string) => void;
  cerrarSesion: () => void;
  autenticado: boolean;
  tieneRol: (rol: RolUsuario) => boolean;
  esGerente: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [usuario, setUsuario] = useState<UsuarioSesion | null>(null);
  const [token, setToken] = useState<string | null>(null);

  const cerrarSesion = useCallback(() => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('roles');
    localStorage.removeItem('userId');
    setToken(null);
    setUsuario(null);
  }, []);

  useEffect(() => {
    const storedToken = localStorage.getItem('token');
    const storedUsername = localStorage.getItem('username');
    const storedRoles = localStorage.getItem('roles');
    const storedUserId = localStorage.getItem('userId');

    if (storedToken && storedUsername && storedRoles && storedUserId) {
      try {
        const decoded = jwtDecode(storedToken);
        if (decoded.exp && decoded.exp * 1000 < Date.now()) {
          cerrarSesion();
        } else {
          setToken(storedToken);
          setUsuario({
            id: Number(storedUserId),
            username: storedUsername,
            roles: storedRoles,
          });
        }
      } catch {
        cerrarSesion();
      }
    }
  }, [cerrarSesion]);

  const iniciarSesion = (newToken: string, userId: number, username: string, roles: string) => {
    localStorage.setItem('token', newToken);
    localStorage.setItem('username', username);
    localStorage.setItem('roles', roles);
    localStorage.setItem('userId', String(userId));
    setToken(newToken);
    setUsuario({ id: userId, username, roles });
  };

  const tieneRol = (rol: RolUsuario) => {
    if (!usuario) return false;
    return usuario.roles.split(',').map((r) => r.trim()).includes(rol);
  };

  return (
    <AuthContext.Provider
      value={{
        usuario,
        token,
        iniciarSesion,
        cerrarSesion,
        autenticado: !!token,
        tieneRol,
        esGerente: tieneRol('ROLE_OWNER'),
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth debe usarse dentro de AuthProvider');
  }
  return context;
};
