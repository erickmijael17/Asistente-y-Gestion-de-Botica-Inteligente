import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import type { RolUsuario } from '../types/domain.types';

interface PrivateRouteProps {
  roles?: RolUsuario[];
}

export const PrivateRoute = ({ roles }: PrivateRouteProps) => {
  const { autenticado, tieneRol } = useAuth();

  if (!autenticado) {
    return <Navigate to="/login" replace />;
  }

  if (roles && !roles.some((rol) => tieneRol(rol))) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
};
