import type { RolUsuario } from '../types/domain.types';

export const separarRoles = (roles: string): RolUsuario[] =>
  roles
    .split(',')
    .map((r) => r.trim())
    .filter(Boolean) as RolUsuario[];

export const tieneRol = (roles: string, rol: RolUsuario): boolean =>
  separarRoles(roles).includes(rol);

export const esGerente = (roles: string): boolean => tieneRol(roles, 'ROLE_OWNER');
