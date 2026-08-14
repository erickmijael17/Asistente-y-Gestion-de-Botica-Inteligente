import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { jwtDecode } from 'jwt-decode';
import { EVENTO_SESION_EXPIRADA } from '../api/client';
import { esGerente, tieneRol } from '../utils/roles';
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

const CLAVES_SESION = ['token', 'username', 'roles', 'userId'] as const;

function limpiarSesionStorage() {
  CLAVES_SESION.forEach((clave) => localStorage.removeItem(clave));
}

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [usuario, setUsuario] = useState<UsuarioSesion | null>(null);
  const [token, setToken] = useState<string | null>(null);

  const cerrarSesion = useCallback(() => {
    limpiarSesionStorage();
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

  useEffect(() => {
    const handleSesionExpirada = () => {
      cerrarSesion();
    };
    window.addEventListener(EVENTO_SESION_EXPIRADA, handleSesionExpirada);
    return () => window.removeEventListener(EVENTO_SESION_EXPIRADA, handleSesionExpirada);
  }, [cerrarSesion]);

  const iniciarSesion = (newToken: string, userId: number, username: string, roles: string) => {
    localStorage.setItem('token', newToken);
    localStorage.setItem('username', username);
    localStorage.setItem('roles', roles);
    localStorage.setItem('userId', String(userId));
    setToken(newToken);
    setUsuario({ id: userId, username, roles });
  };

  const verificarRol = (rol: RolUsuario) => {
    if (!usuario) return false;
    return tieneRol(usuario.roles, rol);
  };

  return (
    <AuthContext.Provider
      value={{
        usuario,
        token,
        iniciarSesion,
        cerrarSesion,
        autenticado: !!token,
        tieneRol: verificarRol,
        esGerente: usuario ? esGerente(usuario.roles) : false,
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