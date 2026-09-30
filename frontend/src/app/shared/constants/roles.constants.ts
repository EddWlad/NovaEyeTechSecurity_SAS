import { Role } from '../../core/models/enums';

export const ROLE_LABELS: Record<Role, string> = {
  ADMINISTRADOR: 'Super administrador',
  ADMIN_OPERATIVO: 'Administrador',
  TECNICO: 'Técnico',
};

/** Solo el super administrador: usuarios, parámetros de cotización y auditoría. */
export const SUPER_ADMIN_ROLES: Role[] = ['ADMINISTRADOR'];

/** Quienes administran catálogos, proveedores y el resto de la operación. */
export const MANAGER_ROLES: Role[] = ['ADMINISTRADOR', 'ADMIN_OPERATIVO'];

/** Todos los perfiles: pantallas compartidas con el técnico. */
export const ALL_ROLES: Role[] = ['ADMINISTRADOR', 'ADMIN_OPERATIVO', 'TECNICO'];
