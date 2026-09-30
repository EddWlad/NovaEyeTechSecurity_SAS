import { Role } from '../models/enums';
import { NAVIGATION_ITEMS } from './navigation.config';

const visibleFor = (role: Role) =>
  NAVIGATION_ITEMS.filter((item) => item.roles.includes(role)).map((item) => item.label);

describe('menu por rol', () => {
  it('el super administrador ve todos los modulos', () => {
    expect(visibleFor('ADMINISTRADOR').length).toBe(NAVIGATION_ITEMS.length);
  });

  it('el administrador ve todo menos usuarios, parametros y auditoria', () => {
    const visible = visibleFor('ADMIN_OPERATIVO');

    expect(visible).not.toContain('Usuarios');
    expect(visible).not.toContain('Parámetros');
    expect(visible).not.toContain('Auditoría');
    expect(visible.length).toBe(NAVIGATION_ITEMS.length - 3);
  });

  it('el tecnico conserva sus modulos', () => {
    expect(visibleFor('TECNICO')).toEqual([
      'Dashboard', 'Mi Perfil', 'Clientes', 'Productos', 'Servicios', 'Cotizaciones', 'Mantenimientos',
    ]);
  });
});
