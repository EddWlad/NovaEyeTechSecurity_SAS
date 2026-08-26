import { NavigationItem } from '../models/navigation.models';

export const NAVIGATION_ITEMS: NavigationItem[] = [
  { label: 'Dashboard', icon: 'dashboard', route: '/dashboard', roles: ['ADMINISTRADOR', 'TECNICO'] },
  { label: 'Usuarios', icon: 'group', route: '/users', roles: ['ADMINISTRADOR'] },
  { label: 'Mi Perfil', icon: 'person', route: '/profile', roles: ['ADMINISTRADOR', 'TECNICO'] },
  { label: 'Clientes', icon: 'domain', route: '/clients', roles: ['ADMINISTRADOR', 'TECNICO'] },
  { label: 'Proveedores', icon: 'local_shipping', route: '/suppliers', roles: ['ADMINISTRADOR'] },
  { label: 'Cat. Productos', icon: 'category', route: '/product-categories', roles: ['ADMINISTRADOR'] },
  { label: 'Productos', icon: 'inventory_2', route: '/products', roles: ['ADMINISTRADOR', 'TECNICO'] },
  { label: 'Cat. Servicios', icon: 'tune', route: '/service-categories', roles: ['ADMINISTRADOR'] },
  { label: 'Servicios', icon: 'engineering', route: '/services', roles: ['ADMINISTRADOR', 'TECNICO'] },
  { label: 'Parámetros', icon: 'settings', route: '/quotation-settings', roles: ['ADMINISTRADOR'] },
  { label: 'Cotizaciones', icon: 'request_quote', route: '/quotations', roles: ['ADMINISTRADOR', 'TECNICO'] },
  { label: 'Mantenimientos', icon: 'build', route: '/maintenance', roles: ['ADMINISTRADOR', 'TECNICO'] },
  { label: 'Auditoría', icon: 'manage_history', route: '/audit-logs', roles: ['ADMINISTRADOR'] },
];
