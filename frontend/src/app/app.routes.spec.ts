import { Route } from '@angular/router';
import { routes } from './app.routes';

const shellChildren = (): Route[] => routes.find((route) => route.path === '')?.children ?? [];

const find = (path: string): Route | undefined => shellChildren().find((route) => route.path === path);

describe('app.routes', () => {
  it('todas las pantallas internas son diferidas y protegidas por rol', () => {
    const screens = shellChildren().filter((route) => !route.redirectTo);

    expect(screens.length).toBeGreaterThan(0);
    for (const route of screens) {
      expect(route.loadComponent).withContext(route.path ?? '').toBeDefined();
      expect(route.canActivate?.length).withContext(route.path ?? '').toBe(1);
      expect(route.data?.['roles']?.length).withContext(route.path ?? '').toBeGreaterThan(0);
    }
  });

  it('cada catalogo tiene lista, nuevo, detalle y edicion con su resourceKey', () => {
    for (const key of ['users', 'clients', 'suppliers', 'product-categories', 'products', 'service-categories', 'services']) {
      for (const path of [key, `${key}/new`, `${key}/:id`, `${key}/:id/edit`]) {
        expect(find(path)?.data?.['resourceKey']).withContext(path).toBe(key);
      }
    }
  });

  it('el tecnico ve productos y servicios pero no los crea ni edita', () => {
    for (const key of ['products', 'services']) {
      expect(find(key)?.data?.['roles']).toEqual(['ADMINISTRADOR', 'TECNICO']);
      expect(find(`${key}/new`)?.data?.['roles']).toEqual(['ADMINISTRADOR']);
      expect(find(`${key}/:id/edit`)?.data?.['roles']).toEqual(['ADMINISTRADOR']);
    }
  });

  it('usuarios, proveedores y auditoria son solo para administradores', () => {
    for (const path of ['users', 'suppliers', 'audit-logs', 'quotation-settings']) {
      expect(find(path)?.data?.['roles']).withContext(path).toEqual(['ADMINISTRADOR']);
    }
  });
});
