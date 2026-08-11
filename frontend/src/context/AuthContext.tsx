import React, { createContext, useContext, useState, useEffect } from 'react';
import { jwtDecode } from 'jwt-decode';

interface User {
  username: string;
  roles: string;
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  login: (token: string, username: string, roles: string) => void;
  logout: () => void;
  isAuthenticated: boolean;
  hasRole: (role: string) => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);

  useEffect(() => {
    const storedToken = localStorage.getItem('token');
    const storedUsername = localStorage.getItem('username');
    const storedRoles = localStorage.getItem('roles');

    if (storedToken && storedUsername && storedRoles) {
      try {
        const decoded = jwtDecode(storedToken);
        // Check if token is expired
        if (decoded.exp && decoded.exp * 1000 < Date.now()) {
          logout();
        } else {
          setToken(storedToken);
          setUser({ username: storedUsername, roles: storedRoles });
        }
      } catch (error) {
        logout();
      }
    }
  }, []);

  const login = (newToken: string, username: string, roles: string) => {
    localStorage.setItem('token', newToken);
    localStorage.setItem('username', username);
    localStorage.setItem('roles', roles);
    setToken(newToken);
    setUser({ username, roles });
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('roles');
    setToken(null);
    setUser(null);
  };

  const hasRole = (role: string) => {
    if (!user) return false;
    return user.roles.split(',').includes(role);
  };

  return (
    <AuthContext.Provider value={{ user, token, login, logout, isAuthenticated: !!token, hasRole }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
