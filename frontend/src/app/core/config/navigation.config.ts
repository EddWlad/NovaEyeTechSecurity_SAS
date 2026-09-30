import { NavigationItem } from '../models/navigation.models';
import { ALL_ROLES, MANAGER_ROLES, SUPER_ADMIN_ROLES } from '../../shared/constants/roles.constants';

export const NAVIGATION_ITEMS: NavigationItem[] = [
  { label: 'Dashboard', icon: 'dashboard', route: '/dashboard', roles: ALL_ROLES },
  { label: 'Usuarios', icon: 'group', route: '/users', roles: SUPER_ADMIN_ROLES },
  { label: 'Mi Perfil', icon: 'person', route: '/profile', roles: ALL_ROLES },
  { label: 'Clientes', icon: 'domain', route: '/clients', roles: ALL_ROLES },
  { label: 'Proveedores', icon: 'local_shipping', route: '/suppliers', roles: MANAGER_ROLES },
  { label: 'Cat. Productos', icon: 'category', route: '/product-categories', roles: MANAGER_ROLES },
  { label: 'Productos', icon: 'inventory_2', route: '/products', roles: ALL_ROLES },
  { label: 'Cat. Servicios', icon: 'tune', route: '/service-categories', roles: MANAGER_ROLES },
  { label: 'Servicios', icon: 'engineering', route: '/services', roles: ALL_ROLES },
  { label: 'Parámetros', icon: 'settings', route: '/quotation-settings', roles: SUPER_ADMIN_ROLES },
  { label: 'Cotizaciones', icon: 'request_quote', route: '/quotations', roles: ALL_ROLES },
  { label: 'Mantenimientos', icon: 'build', route: '/maintenance', roles: ALL_ROLES },
  { label: 'Auditoría', icon: 'manage_history', route: '/audit-logs', roles: SUPER_ADMIN_ROLES },
];
